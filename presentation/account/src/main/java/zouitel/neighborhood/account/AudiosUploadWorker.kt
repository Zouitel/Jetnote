package zouitel.neighborhood.account

import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.media.MediaHttpUploader
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
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile

class AudiosUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            uploadsAudios(applicationContext, initializeDriveService())
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    suspend fun initializeDriveService(): Drive? = withContext(ioDispatcher) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf(DriveScopes.DRIVE_FILE) // Changed from DRIVE_PHOTOS_READONLY to DRIVE_FILE
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
                    credential
                ).setApplicationName("Drive File Manager").build()
            }
        } catch (e: Exception) {
            Log.e("AudiosUploadWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    @WorkerThread
    suspend fun uploadsAudios(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        if (driveService == null) {
            Log.e("AudiosUploadWorker", "Drive service is null, cannot upload Audios")
            return@withContext
        }

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
        if (mediasFolderId == null) {
            Log.e("AudiosUploadWorker", "Root folder 'jetnote_medias' not found in Drive.")
            return@withContext
        }

        val audiosFolderId = findFolderId(driveService, "audios", mediasFolderId)
        if (audiosFolderId == null) {
            Log.e("AudiosUploadWorker", "Audios folder not found in Drive.")
            return@withContext
        }

        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
        if (mediaDir?.exists() != true || !mediaDir.isDirectory) {
            Log.e("AudiosUploadWorker", "Audios directory not found: ${mediaDir?.absolutePath}")
            return@withContext
        }

        val files = mediaDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("AudiosUploadWorker", "No files found in Audios directory")
            return@withContext
        }

        // Use coroutineScope to properly await all jobs
        val uploadJobs = files.mapNotNull { localMediaFile ->
            if (localMediaFile.isFile) {
                async {
                    try {
                        val query = "name='${localMediaFile.name}' and '$audiosFolderId' in parents and trashed=false"
                        val result = driveService.files().list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute()

                        if (result.files.isNullOrEmpty()) {
                            updateFile(driveService, localMediaFile, audiosFolderId)
                            Log.d("AudiosUploadWorker", "Uploaded: ${localMediaFile.name}")
                        } else {
                            Log.d("AudiosUploadWorker", "File already exists: ${localMediaFile.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("AudiosUploadWorker", "Failed to upload ${localMediaFile.name}: ${e.message}", e)
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

                // Get correct MIME type for audio files
                val mimeType = getMimeType(localFile) ?: "audio/*"
                val mediaContent = FileContent(mimeType, localFile)

                if (existingFileId != null) {
                    // File exists, update it
                    val updateRequest = driveService.files().update(existingFileId, null, mediaContent)
                    // Configure chunked upload for large audio files
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
                    // Configure chunked upload for large audio files
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
}