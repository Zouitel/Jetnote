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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudiosDownloadsWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("AudiosDownloadWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            downloadAudios(applicationContext, initializeDriveService())
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("AudiosDownloadsWorker", "Download failed (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e("AudiosDownloadsWorker", "Download failed: ${e.message}", e)
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
        } catch (e: Exception) {
            Log.e("AudiosDownloadsWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    @WorkerThread
    suspend fun downloadAudios(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        // A missing service or folder must fail the work: returning normally here
        // used to report a successful sync while nothing was downloaded.
        val drive = driveService ?: throw IOException("Drive service is not available")

        val mediasFolderId = findFolderId(drive, "jetnote_medias")
            ?: throw IOException("Root folder 'jetnote_medias' not found in Drive")

        val audiosFolderId = findFolderId(drive, "audios", mediasFolderId)
            ?: throw IOException("Audios folder not found in Drive")

        val audiosDir = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
            ?: throw IOException("Audios directory not accessible")

        if (!audiosDir.exists()) {
            audiosDir.mkdirs()
        }

        val query = "'$audiosFolderId' in parents and trashed=false"
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name, mimeType)")
            .execute()

        val driveFiles = result.files ?: emptyList()

        if (driveFiles.isEmpty()) {
            Log.d("AudiosDownloadsWorker", "No files found in Drive audios folder")
            return@withContext
        }

        Log.d("AudiosDownloadsWorker", "Found ${driveFiles.size} files in Drive audios folder")

        // A failed download fails the worker so WorkManager can retry it;
        // already completed files are skipped on retry.
        coroutineScope {
            driveFiles.map { driveFile ->
                async { downloadFile(drive, driveFile.id, driveFile.name, audiosDir) }
            }.awaitAll()
        }

        Log.d("AudiosDownloadsWorker", "All download files completed")
    }

    private suspend fun downloadFile(
        driveService: Drive,
        fileId: String,
        fileName: String,
        localDir: File
    ) = withContext(Dispatchers.IO) {
        val localFile = File(localDir, fileName)

        if (localFile.exists()) {
            Log.d("AudiosDownloadsWorker", "File already exists, skipping: $fileName")
            return@withContext
        }

        // Download to a temporary file first so an interrupted transfer can never
        // leave a truncated file behind that the exists() check above would then
        // treat as complete and refuse to re-download.
        val partialFile = File(localDir, "$fileName.part")

        try {
            FileOutputStream(partialFile).use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.flush()
            }

            if (!partialFile.renameTo(localFile)) {
                throw IOException("Could not move downloaded file into place: $fileName")
            }

            Log.d("AudiosDownloadsWorker", "Downloaded: $fileName to ${localFile.absolutePath}")
        } catch (e: Exception) {
            partialFile.delete()
            throw e
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
            Log.e("AudiosDownloadsWorker", "Failed to find folder '$folderName': ${e.message}", e)
            null
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}