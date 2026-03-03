package zouitel.neighborhood.account.worker

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PicturesDownloadsWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            downloadPictures(applicationContext, initializeDriveService())
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
    suspend fun downloadPictures(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        if (driveService == null) {
            Log.e("PicturesDownloadsWorker", "Drive service is null")
            return@withContext
        }

        // Find root media folder
        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
        if (mediasFolderId == null) {
            Log.e("PicturesDownloadsWorker", "Root folder 'jetnote_medias' not found in Drive")
            return@withContext
        }

        // Find pictures subfolder
        val picturesFolderId = findFolderId(driveService, "pictures", mediasFolderId)
        if (picturesFolderId == null) {
            Log.e("PicturesDownloadsWorker", "Pictures folder not found in Drive")
            return@withContext
        }

        // Get local pictures directory
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (picturesDir == null) {
            Log.e("PicturesDownloadsWorker", "Pictures directory not accessible")
            return@withContext
        }

        // Create directory if it doesn't exist
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }

        // Get all image files from Drive pictures folder
        val downloadJobs = mutableListOf<Deferred<Unit>>()

        try {
            val query = "'$picturesFolderId' in parents and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType)")
                .execute()

            val driveFiles = result.files ?: emptyList()

            if (driveFiles.isEmpty()) {
                Log.d("PicturesDownloadsWorker", "No pictures found in Drive folder")
                return@withContext
            }

            Log.d("PicturesDownloadsWorker", "Found ${driveFiles.size} files in Drive pictures folder")

            // Download each picture
            driveFiles.forEach { driveFile ->
                // Filter only image files
                if (isImageFile(driveFile.name, driveFile.mimeType)) {
                    downloadJobs.add(async {
                        downloadFile(driveService, driveFile.id, driveFile.name, picturesDir)
                    })
                }
            }

            // Wait for all downloads to complete
            downloadJobs.forEach { it.await() }
            Log.d("PicturesDownloadsWorker", "All picture downloads completed")

        } catch (e: Exception) {
            Log.e("PicturesDownloadsWorker", "Error listing files from Drive: ${e.message}", e)
        }
    }

    private fun isImageFile(fileName: String, mimeType: String?): Boolean {
        val imageExtensions = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "heic", "heif", "svg", "tiff", "tif"
        )

        val extension = fileName.substringAfterLast('.', "").lowercase()
        val hasImageExtension = extension in imageExtensions
        val hasImageMimeType = mimeType?.startsWith("image/") == true

        return hasImageExtension || hasImageMimeType
    }

    private suspend fun downloadFile(
        driveService: Drive,
        fileId: String,
        fileName: String,
        localDir: File
    ) = withContext(Dispatchers.IO) {
        try {
            val localFile = File(localDir, fileName)

            // Skip if file already exists
            if (localFile.exists()) {
                Log.d("PicturesDownloadsWorker", "File already exists, skipping: $fileName")
                return@withContext
            }

            // Download file from Drive
            FileOutputStream(localFile).use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.flush()
            }

            Log.d("PicturesDownloadsWorker", "Downloaded: $fileName to ${localFile.absolutePath}")

        } catch (e: IOException) {
            Log.e("PicturesDownloadsWorker", "Error downloading file $fileName: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("PicturesDownloadsWorker", "Unexpected error downloading $fileName: ${e.message}", e)
        }
    }

    private suspend fun findFolderId(
        driveService: Drive,
        folderName: String,
        parentId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val query = if (parentId != null) {
                "name='$folderName' and '$parentId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"
            } else {
                "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            }

            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            result.files?.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("PicturesDownloadsWorker", "Failed to find folder '$folderName': ${e.message}", e)
            null
        }
    }
}