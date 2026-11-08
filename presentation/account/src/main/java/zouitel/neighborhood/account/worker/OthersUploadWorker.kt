package zouitel.neighborhood.account.worker

import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile
import java.io.IOException

class OthersUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("OthersUploadWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            uploadsDownloads(applicationContext, initializeDriveService())
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("OthersUploadWorker", "Upload failed (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e("OthersUploadWorker", "Upload failed: ${e.message}", e)
            Result.failure()
        }
    }

    suspend fun initializeDriveService(): Drive? = withContext(ioDispatcher) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf(DriveScopes.DRIVE_FILE)
            )

            val lastAccount = GoogleSignIn.getLastSignedInAccount(applicationContext)
            credential.selectedAccount = lastAccount?.account

            credential?.run {
                Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    AccountBoundDriveRequestInitializer(credential, applicationContext, inputData)
                ).setApplicationName("Drive File Manager").build()
            }
        } catch (_: Exception) {
            null
        }
    }

    @WorkerThread
    suspend fun uploadsDownloads(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        // A missing service or folder must fail the work: returning normally here
        // used to report a successful upload while nothing was uploaded.
        if (driveService == null) throw IOException("Google Drive account is not available")

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
            ?: throw IOException("Root folder 'jetnote_medias' not found in Drive")

        val downloadsFolderId = findFolderId(driveService, "downloads", mediasFolderId)
            ?: throw IOException("Downloads folder not found in Drive")

        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (mediaDir?.exists() != true || !mediaDir.isDirectory) {
            throw IOException("Downloads directory not accessible: ${mediaDir?.absolutePath}")
        }

        val files = mediaDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("OthersUploadWorker", "No files found in downloads directory")
            return@withContext
        }

        // A failed upload fails the worker so WorkManager can retry it;
        // files already uploaded are skipped on the next attempt.
        coroutineScope {
            val uploadJobs = files.mapNotNull { localMediaFile ->
                if (localMediaFile.isFile) {
                    async {
                        val query =
                            "name='${localMediaFile.name}' and '$downloadsFolderId' in parents and trashed=false"
                        val result = driveService.files().list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute()

                        if (result.files.isNullOrEmpty()) {
                            updateFile(driveService, localMediaFile, downloadsFolderId)
                        }
                    }
                } else {
                    null
                }
            }
            uploadJobs.awaitAll()
        }

        Log.d("OthersUploadWorker", "All upload jobs completed")
    }

    private fun getMimeType(file: JavaFile): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream" // Default fallback for unknown types
    }

    private suspend fun updateFile(driveService: Drive, localFile: JavaFile, folderId: String) {
        if (!localFile.exists()) {
            Log.e("SyncScreenModel", "File not found for upload: ${localFile.absolutePath}")
            return
        }

        withContext(Dispatchers.IO) {
            // Check if the file already exists in Google Drive
            val query = "name='${localFile.name}' and '$folderId' in parents and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            val existingFileId = result.files?.firstOrNull()?.id
            val mimeType = getMimeType(localFile)
            val mediaContent = FileContent(mimeType, localFile)

            if (existingFileId != null) {
                // File exists, update it
                driveService.files().update(existingFileId, null, mediaContent).execute()
                Log.d("SyncScreenModel", "File updated: ${localFile.name} (${mimeType})")
            } else {
                // File does not exist, create it
                val fileMetadata = DriveFile().apply {
                    name = localFile.name
                    parents = listOf(folderId)
                }
                driveService.files().create(fileMetadata, mediaContent).execute()
                Log.d("SyncScreenModel", "File created: ${localFile.name} (${mimeType})")
            }
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}