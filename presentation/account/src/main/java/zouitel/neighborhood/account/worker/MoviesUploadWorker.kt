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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
import java.net.SocketTimeoutException
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile

class MoviesUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("MoviesUploadWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            uploadsMovies(applicationContext, initializeDriveService())
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
            if (lastAccount == null) {
                Log.e("MoviesUploadWorker", "No signed-in Google account found")
                return@withContext null
            }

            credential.selectedAccount = lastAccount.account

            credential.run {
                Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("Drive File Manager").build()
            }
        } catch (e: Exception) {
            Log.e("MoviesUploadWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    @WorkerThread
    suspend fun uploadsMovies(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        if (driveService == null) {
            Log.e("MoviesUploadWorker", "Drive service is null, cannot upload movies")
            return@withContext
        }

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
        if (mediasFolderId == null) {
            Log.e("MoviesUploadWorker", "Root folder 'jetnote_medias' not found in Drive.")
            return@withContext
        }

        val moviesFolderId = findFolderId(driveService, "movies", mediasFolderId)
        if (moviesFolderId == null) {
            Log.e("MoviesUploadWorker", "Movies folder not found in Drive.")
            return@withContext
        }

        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (mediaDir?.exists() != true || !mediaDir.isDirectory) {
            Log.e("MoviesUploadWorker", "Movies directory not found: ${mediaDir?.absolutePath}")
            return@withContext
        }

        val files = mediaDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("MoviesUploadWorker", "No files found in movies directory")
            return@withContext
        }

        Log.d("MoviesUploadWorker", "Found ${files.size} files to process")

        // Use coroutineScope to properly await all jobs
        coroutineScope {
            val uploadJobs = files.mapNotNull { localMediaFile ->
                if (localMediaFile.isFile) {
                    async {
                        try {
                            // Check file size before uploading
                            val fileSizeMB = localMediaFile.length() / (1024.0 * 1024.0)
                            Log.d("MoviesUploadWorker", "Processing ${localMediaFile.name} (${String.format("%.2f", fileSizeMB)} MB)")

                            val query = "name='${localMediaFile.name}' and '$moviesFolderId' in parents and trashed=false"
                            val result = driveService.files().list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute()

                            if (result.files.isNullOrEmpty()) {
                                updateFileWithRetry(driveService, localMediaFile, moviesFolderId)
                                Log.d("MoviesUploadWorker", "Uploaded: ${localMediaFile.name}")
                            } else {
                                Log.d("MoviesUploadWorker", "File already exists: ${localMediaFile.name}")
                            }
                        } catch (e: Exception) {
                            Log.e("MoviesUploadWorker", "Failed to upload ${localMediaFile.name}: ${e.message}", e)
                        }
                    }
                } else {
                    null
                }
            }

            // Wait for all upload jobs to complete
            uploadJobs.awaitAll()
        }

        Log.d("MoviesUploadWorker", "All upload jobs completed")
    }

    private suspend fun updateFileWithRetry(
        driveService: Drive,
        localFile: JavaFile,
        folderId: String,
        maxRetries: Int = 3
    ) {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                if (attempt > 0) {
                    Log.d("MoviesUploadWorker", "Retry attempt ${attempt + 1} for ${localFile.name}")
                    delay(2000L * attempt) // Exponential backoff
                }

                updateFile(driveService, localFile, folderId)
                return // Success, exit function

            } catch (e: SocketTimeoutException) {
                lastException = e
                Log.w("MoviesUploadWorker", "Timeout on attempt ${attempt + 1} for ${localFile.name}")
            } catch (e: Exception) {
                lastException = e
                Log.e("MoviesUploadWorker", "Error on attempt ${attempt + 1} for ${localFile.name}: ${e.message}")
                throw e // Re-throw non-timeout exceptions immediately
            }
        }

        // All retries failed
        Log.e("MoviesUploadWorker", "Failed to upload ${localFile.name} after $maxRetries attempts")
        throw lastException ?: Exception("Upload failed after $maxRetries attempts")
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

                // Get correct MIME type for video files
                val mimeType = getVideoMimeType(localFile.name)
                val mediaContent = FileContent(mimeType, localFile)

                if (existingFileId != null) {
                    // File exists, update it
                    val updateRequest = driveService.files().update(existingFileId, null, mediaContent)
                    // Configure chunked upload for large video files
                    updateRequest.mediaHttpUploader?.apply {
                        isDirectUploadEnabled = false
                        chunkSize = 100 * 1024 * 1024
                    }
                    updateRequest.execute()
                    Log.d("MoviesUploadWorker", "File updated: ${localFile.name}")
                } else {
                    // File does not exist, create it
                    val fileMetadata = DriveFile().apply {
                        name = localFile.name
                        parents = listOf(folderId)
                    }
                    val createRequest = driveService.files().create(fileMetadata, mediaContent)
                    // Configure chunked upload for large video files
                    createRequest.mediaHttpUploader?.apply {
                        isDirectUploadEnabled = false
                        chunkSize = 100 * 1024 * 1024
                    }
                    createRequest.execute()
                    Log.d("MoviesUploadWorker", "File created: ${localFile.name}")
                }
            } catch (e: Exception) {
                error(e.message ?: "error!")
            }
        }
    }

    /**
     * Get the appropriate MIME type for video files
     */
    private fun getVideoMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "flv" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            "3gp" -> "video/3gpp"
            "m4v" -> "video/x-m4v"
            "mpeg", "mpg" -> "video/mpeg"
            else -> "video/mp4" // Default to mp4 for unknown video types
        }
    }
}