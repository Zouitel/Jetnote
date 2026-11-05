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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile

class OthersUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData)) {
            Log.w("OthersUploadWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            uploadsDownloads(applicationContext, initializeDriveService())
            Result.success()
        } catch (_: Exception) {
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
                    credential
                ).setApplicationName("Drive File Manager").build()
            }
        } catch (_: Exception) {
            null
        }
    }

    @WorkerThread
    suspend fun uploadsDownloads(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        if (driveService == null) return@withContext

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
        if (mediasFolderId == null) {
            Log.e("SyncScreenModel", "Root folder 'Jetnote_medias' not found in Drive.")
            return@withContext
        }

        val uploadJobs = mutableListOf<Deferred<Unit>>()
        val downloadsFolderId = findFolderId(driveService, "downloads", mediasFolderId)
        downloadsFolderId?.let { mFolderId ->
            val mediaDir = Environment.DIRECTORY_DOWNLOADS.let { context.getExternalFilesDir(it) }
            if (mediaDir?.exists() == true && mediaDir.isDirectory) {
                mediaDir.listFiles()?.forEach { localMediaFile ->
                    if (localMediaFile.isFile) {
                        uploadJobs.add(async {
                            val query =
                                "name='${localMediaFile.name}' and '$mFolderId' in parents and trashed=false"
                            val result = driveService.files().list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute()

                            if (result.files.isNullOrEmpty()) {
                                updateFile(driveService, localMediaFile, mFolderId)
                            }
                        })
                    }
                }
            } else {
                Log.e(
                    "DataUploadWorker",
                    "Downloads directory not found: ${mediaDir?.absolutePath}"
                )
            }
        }
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
}