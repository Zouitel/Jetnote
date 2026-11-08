package zouitel.neighborhood.account.worker

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.escapeDriveQueryLiteral
import zouitel.neighborhood.account.utils.findFolderId
import java.net.SocketTimeoutException
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile
import java.io.IOException

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
            uploadsMovies(applicationContext, initializeDriveService(applicationContext, inputData, "MoviesUploadWorker", ioDispatcher))
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("MoviesUploadWorker", "Upload failed (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e("MoviesUploadWorker", "Upload failed: ${e.message}", e)
            Result.failure()
        }
    }

    @WorkerThread
    suspend fun uploadsMovies(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        // A missing service or folder must fail the work: returning normally here
        // used to report a successful upload while nothing was uploaded.
        if (driveService == null) throw IOException("Google Drive account is not available")

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
            ?: throw IOException("Root folder 'jetnote_medias' not found in Drive.")

        val moviesFolderId = findFolderId(driveService, "movies", mediasFolderId)
            ?: throw IOException("Movies folder not found in Drive.")

        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (mediaDir?.exists() != true || !mediaDir.isDirectory) {
            throw IOException("Movies directory not found: ${mediaDir?.absolutePath}")
        }

        val files = mediaDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("MoviesUploadWorker", "No files found in movies directory")
            return@withContext
        }

        Log.d("MoviesUploadWorker", "Found ${files.size} files to process")

        // Use coroutineScope to properly await all jobs
        // Cap concurrent Drive requests to stay clear of API rate limits.
        val uploadSemaphore = Semaphore(MAX_CONCURRENT_TRANSFERS)
        coroutineScope {
            val uploadJobs = files.mapNotNull { localMediaFile ->
                if (localMediaFile.isFile) {
                    async {
                        uploadSemaphore.withPermit {
                            // A failed upload fails the worker so WorkManager can retry it;
                            // files already uploaded are skipped on the next attempt.
                            // Check file size before uploading
                            val fileSizeMB = localMediaFile.length() / (1024.0 * 1024.0)
                            Log.d("MoviesUploadWorker", "Processing ${localMediaFile.name} (${String.format("%.2f", fileSizeMB)} MB)")

                            val query = "name='${escapeDriveQueryLiteral(localMediaFile.name)}' and '$moviesFolderId' in parents and trashed=false"
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

                createFile(driveService, localFile, folderId)
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

    private suspend fun createFile(driveService: Drive, localFile: JavaFile, folderId: String) {
        if (!localFile.exists()) {
            throw IOException("File not found for upload: ${localFile.absolutePath}")
        }

        withContext(Dispatchers.IO) {
            val mimeType = getVideoMimeType(localFile.name)
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
            Log.d("MoviesUploadWorker", "File created: ${localFile.name}")
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

    private companion object {
        const val MAX_RETRIES = 3
        const val MAX_CONCURRENT_TRANSFERS = 3

        /** Files above this size are uploaded with the resumable protocol. */
        const val RESUMABLE_UPLOAD_THRESHOLD_BYTES = 16L * 1024 * 1024

        /** Resumable uploads buffer one chunk in memory, so keep this modest. */
        const val RESUMABLE_UPLOAD_CHUNK_BYTES = 8 * 1024 * 1024
    }
}