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

class OthersDownloadsWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("OthersDownloadsWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            downloadOthers(applicationContext, initializeDriveService())
            Result.success()
        } catch (e: Exception) {
            Log.e("MoviesDownloadsWorker", "Download failed: ${e.message}", e)
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
        } catch (e: Exception) {
            Log.e("MoviesDownloadsWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    @WorkerThread
    suspend fun downloadOthers(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        if (driveService == null) {
            Log.e("MoviesDownloadsWorker", "Drive service is null")
            return@withContext
        }

        val mediasFolderId = findFolderId(driveService, "jetnote_medias")
        if (mediasFolderId == null) {
            Log.e("MoviesDownloadsWorker", "Root folder 'jetnote_medias' not found in Drive")
            return@withContext
        }

        val downloadsFolderId = findFolderId(driveService, "downloads", mediasFolderId)
        if (downloadsFolderId == null) {
            Log.e("MoviesDownloadsWorker", "Downloads folder not found in Drive")
            return@withContext
        }

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir == null) {
            Log.e("MoviesDownloadsWorker", "Downloads directory not accessible")
            return@withContext
        }

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val downloadJobs = mutableListOf<Deferred<Unit>>()

        try {
            val query = "'$downloadsFolderId' in parents and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType)")
                .execute()

            val driveFiles = result.files ?: emptyList()

            if (driveFiles.isEmpty()) {
                Log.d("MoviesDownloadsWorker", "No files found in Drive downloads folder")
                return@withContext
            }

            Log.d("MoviesDownloadsWorker", "Found ${driveFiles.size} files in Drive downloads folder")

            // Download all files (any type)
            driveFiles.forEach { driveFile ->
                downloadJobs.add(async {
                    downloadFile(driveService, driveFile.id, driveFile.name, downloadsDir)
                })
            }

            downloadJobs.forEach { it.await() }
            Log.d("MoviesDownloadsWorker", "All download files completed")

        } catch (e: Exception) {
            Log.e("MoviesDownloadsWorker", "Error listing files from Drive: ${e.message}", e)
        }
    }

    private suspend fun downloadFile(
        driveService: Drive,
        fileId: String,
        fileName: String,
        localDir: File
    ) = withContext(Dispatchers.IO) {
        try {
            val localFile = File(localDir, fileName)

            if (localFile.exists()) {
                Log.d("MoviesDownloadsWorker", "File already exists, skipping: $fileName")
                return@withContext
            }

            FileOutputStream(localFile).use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.flush()
            }

            Log.d("MoviesDownloadsWorker", "Downloaded: $fileName to ${localFile.absolutePath}")

        } catch (e: IOException) {
            Log.e("MoviesDownloadsWorker", "Error downloading file $fileName: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("MoviesDownloadsWorker", "Unexpected error downloading $fileName: ${e.message}", e)
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
            Log.e("MoviesDownloadsWorker", "Failed to find folder '$folderName': ${e.message}", e)
            null
        }
    }
}