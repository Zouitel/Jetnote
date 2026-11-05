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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile

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
        } catch (e: Exception) {
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
        if (driveService == null) {
            Log.e("PicturesUploadWorker", "Drive service is null, cannot upload pictures")
            return@withContext
        }

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
        if (mediasFolderId == null) {
            Log.e("PicturesUploadWorker", "Root folder 'jetnote_medias' not found in Drive.")
            return@withContext
        }

        val picturesFolderId = findFolderId(driveService, "pictures", mediasFolderId)
        if (picturesFolderId == null) {
            Log.e("PicturesUploadWorker", "Pictures folder not found in Drive.")
            return@withContext
        }

        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (mediaDir?.exists() != true || !mediaDir.isDirectory) {
            Log.e("PicturesUploadWorker", "Pictures directory not found: ${mediaDir?.absolutePath}")
            return@withContext
        }

        val files = mediaDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("PicturesUploadWorker", "No files found in pictures directory")
            return@withContext
        }

        // Use coroutineScope to properly await all jobs
        val uploadJobs = files.mapNotNull { localMediaFile ->
            if (localMediaFile.isFile) {
                async {
                    try {
                        val query = "name='${localMediaFile.name}' and '$picturesFolderId' in parents and trashed=false"
                        val result = driveService.files().list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute()

                        if (result.files.isNullOrEmpty()) {
                            updateFile(driveService, localMediaFile, picturesFolderId)
                            Log.d("PicturesUploadWorker", "Uploaded: ${localMediaFile.name}")
                        } else {
                            Log.d("PicturesUploadWorker", "File already exists: ${localMediaFile.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("PicturesUploadWorker", "Failed to upload ${localMediaFile.name}: ${e.message}", e)
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

    private suspend fun updateFile(driveService: Drive, localFile: JavaFile, folderId: String) {
        if (!localFile.exists()) {
            Log.e("MoviesUploadWorker", "File not found for upload: ${localFile.absolutePath}")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                // Check if the file already exists in Google Drive
                val query = "name='${localFile.name}' and '$folderId' in parents and trashed=false"
                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute()

                val existingFileId = result.files?.firstOrNull()?.id

                // Determine the correct MIME type based on file extension
                val mimeType = getMimeType(localFile.name)
                val mediaContent = FileContent(mimeType, localFile)

                if (existingFileId != null) {
                    // File exists, update it
                    val updateRequest = driveService.files().update(existingFileId, null, mediaContent)
                    // Configure chunked upload for large image files
                    updateRequest.mediaHttpUploader?.apply {
                        isDirectUploadEnabled = false
                        chunkSize = 100 * 1024 * 1024
                    }
                    updateRequest.execute()
                } else {
                    // File does not exist, create it
                    val fileMetadata = DriveFile().apply {
                        name = localFile.name
                        parents = listOf(folderId)
                    }
                    val createRequest = driveService.files().create(fileMetadata, mediaContent)
                    // Configure chunked upload for large image files
                    createRequest.mediaHttpUploader?.apply {
                        isDirectUploadEnabled = false
                        chunkSize = 100 * 1024 * 1024
                    }
                    createRequest.execute()
                }
            } catch (e: Exception) {
                error(e.message ?: "error!")
            }
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
}