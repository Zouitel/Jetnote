package zouitel.neighborhood.account.worker

import android.content.Context
import android.os.Environment
import android.util.Log
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.escapeDriveQueryLiteral
import zouitel.neighborhood.account.utils.findFolderId
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile
import java.io.IOException

class PicturesUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("PicturesUploadWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            uploadsPictures(applicationContext, initializeDriveService())
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("PicturesUploadWorker", "Upload failed (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e("PicturesUploadWorker", "Upload failed: ${e.message}", e)
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
            if (lastAccount == null) {
                return@withContext null
            }

            credential.selectedAccount = lastAccount.account

            credential.run {
                Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    AccountBoundDriveRequestInitializer(credential, applicationContext, inputData)
                ).setApplicationName("Drive File Manager").build()
            }
        } catch (e: Exception) {
            Log.e("PicturesUploadWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    @WorkerThread
    suspend fun uploadsPictures(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        // A missing service or folder must fail the work: returning normally here
        // used to report a successful upload while nothing was uploaded.
        if (driveService == null) throw IOException("Google Drive account is not available")

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
            ?: throw IOException("Root folder 'jetnote_medias' not found in Drive.")

        val picturesFolderId = findFolderId(driveService, "pictures", mediasFolderId)
            ?: throw IOException("Pictures folder not found in Drive.")

        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (mediaDir?.exists() != true || !mediaDir.isDirectory) {
            throw IOException("Pictures directory not found: ${mediaDir?.absolutePath}")
        }

        val files = mediaDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("PicturesUploadWorker", "No files found in pictures directory")
            return@withContext
        }

        // Use coroutineScope to properly await all jobs
        // Cap concurrent Drive requests to stay clear of API rate limits.
        val uploadSemaphore = Semaphore(MAX_CONCURRENT_TRANSFERS)
        val uploadJobs = files.mapNotNull { localMediaFile ->
            if (localMediaFile.isFile) {
                async {
                    uploadSemaphore.withPermit {
                        // A failed upload fails the worker so WorkManager can retry it;
                        // files already uploaded are skipped on the next attempt.
                        val query = "name='${escapeDriveQueryLiteral(localMediaFile.name)}' and '$picturesFolderId' in parents and trashed=false"
                        val result = driveService.files().list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute()

                        if (result.files.isNullOrEmpty()) {
                            createFile(driveService, localMediaFile, picturesFolderId)
                            Log.d("PicturesUploadWorker", "Uploaded: ${localMediaFile.name}")
                        } else {
                            Log.d("PicturesUploadWorker", "File already exists: ${localMediaFile.name}")
                        }
                    }
                }
            } else {
                null
            }
        }

        // Wait for all upload jobs to complete
        uploadJobs.awaitAll()

        Log.d("PicturesUploadWorker", "All upload jobs completed")
    }

    private suspend fun createFile(driveService: Drive, localFile: JavaFile, folderId: String) {
        if (!localFile.exists()) {
            throw IOException("File not found for upload: ${localFile.absolutePath}")
        }

        withContext(Dispatchers.IO) {
            val mimeType = getMimeType(localFile.name)
            val mediaContent = FileContent(mimeType, localFile)

            // The caller already confirmed this file is absent from Drive, so one
            // listing per file is enough; create it without re-checking existence.
            val fileMetadata = DriveFile().apply {
                name = localFile.name
                parents = listOf(folderId)
            }
            val createRequest = driveService.files().create(fileMetadata, mediaContent)
            createRequest.mediaHttpUploader?.apply {
                // The uploader buffers a whole chunk in memory, and 100 MB chunks were an
                // OOM waiting to happen on mid-range devices. Small files go out in one
                // direct request; larger ones use resumable 8 MB chunks.
                if (localFile.length() > RESUMABLE_UPLOAD_THRESHOLD_BYTES) {
                    isDirectUploadEnabled = false
                    chunkSize = RESUMABLE_UPLOAD_CHUNK_BYTES
                }
            }
            createRequest.execute()
            Log.d("PicturesUploadWorker", "File created: ${localFile.name}")
        }
    }


    /**
     * Get the appropriate MIME type for image files
     */
    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            else -> "application/octet-stream"
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val MAX_CONCURRENT_TRANSFERS = 3

        /** Files above this size are uploaded with the resumable protocol. */
        const val RESUMABLE_UPLOAD_THRESHOLD_BYTES = 16L * 1024 * 1024

        /** Resumable uploads buffer one chunk in memory, so keep this modest. */
        const val RESUMABLE_UPLOAD_CHUNK_BYTES = 8 * 1024 * 1024
    }
}