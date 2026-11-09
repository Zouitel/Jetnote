package zouitel.neighborhood.account.worker

import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.escapeDriveQueryLiteral
import zouitel.neighborhood.account.utils.findFolderId
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile
import java.io.IOException

class AudiosUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("AudiosUploadWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            uploadsAudios(applicationContext, initializeDriveService(applicationContext, inputData, "AudiosUploadWorker", ioDispatcher))
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("AudiosUploadWorker", "Upload failed (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e("AudiosUploadWorker", "Upload failed: ${e.message}", e)
            Result.failure()
        }
    }

    @WorkerThread
    suspend fun uploadsAudios(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        // A missing service or folder must fail the work: returning normally here
        // used to report a successful upload while nothing was uploaded.
        if (driveService == null) throw IOException("Google Drive account is not available")

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
            ?: throw IOException("Root folder 'jetnote_medias' not found in Drive.")

        val audiosFolderId = findFolderId(driveService, "audios", mediasFolderId)
            ?: throw IOException("Audios folder not found in Drive.")

        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
        if (mediaDir?.exists() != true || !mediaDir.isDirectory) {
            throw IOException("Audios directory not found: ${mediaDir?.absolutePath}")
        }

        val files = mediaDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("AudiosUploadWorker", "No files found in Audios directory")
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
                        val query = "name='${escapeDriveQueryLiteral(localMediaFile.name)}' and '$audiosFolderId' in parents and trashed=false"
                        val result = driveService.files().list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute()

                        if (result.files.isNullOrEmpty()) {
                            createFile(driveService, localMediaFile, audiosFolderId)
                            Log.d("AudiosUploadWorker", "Uploaded: ${localMediaFile.name}")
                        } else {
                            Log.d("AudiosUploadWorker", "File already exists: ${localMediaFile.name}")
                        }
                    }
                }
            } else {
                null
            }
        }

        // Wait for all upload jobs to complete
        uploadJobs.awaitAll()

        Log.d("AudiosUploadWorker", "All upload jobs completed")
    }

    private suspend fun createFile(driveService: Drive, localFile: JavaFile, folderId: String) {
        if (!localFile.exists()) {
            throw IOException("File not found for upload: ${localFile.absolutePath}")
        }

        withContext(Dispatchers.IO) {
            val mimeType = getMimeType(localFile) ?: "audio/*"
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
        }
    }


    /**
     * Get the appropriate MIME type for image files
     * @return the mime type of the file, or null if it's not found.
     */
    private fun getMimeType(file: JavaFile): String? {
        val extension = file.extension.lowercase()
        // The MimeTypeMap might not be exhaustive, so we can add common audio types.
        return when (extension) {
            "mp3", "mpeg" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/x-wav"
            "m4a" -> "audio/mp4"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
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