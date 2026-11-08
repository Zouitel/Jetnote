package zouitel.neighborhood.account.worker

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
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
            downloadOthers(applicationContext, initializeDriveService(applicationContext, inputData, "OthersDownloadsWorker", ioDispatcher))
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("OthersDownloadsWorker", "Download failed (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e("OthersDownloadsWorker", "Download failed: ${e.message}", e)
            Result.failure()
        }
    }

    @WorkerThread
    suspend fun downloadOthers(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        // A missing service or folder must fail the work: returning normally here
        // used to report a successful sync while nothing was downloaded.
        val drive = driveService ?: throw IOException("Drive service is not available")

        val mediasFolderId = findFolderId(drive, "jetnote_medias")
            ?: throw IOException("Root folder 'jetnote_medias' not found in Drive")

        val downloadsFolderId = findFolderId(drive, "downloads", mediasFolderId)
            ?: throw IOException("Downloads folder not found in Drive")

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IOException("Downloads directory not accessible")

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val query = "'$downloadsFolderId' in parents and trashed=false"
        // Drive caps list responses (100 entries by default); follow the page tokens
        // so a folder holding more files is never silently truncated.
        val driveFiles = mutableListOf<com.google.api.services.drive.model.File>()
        var pageToken: String? = null
        do {
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType), nextPageToken")
                .setPageSize(LIST_PAGE_SIZE)
                .setPageToken(pageToken)
                .execute()
            driveFiles += result.files ?: emptyList()
            pageToken = result.nextPageToken
        } while (pageToken != null)

        if (driveFiles.isEmpty()) {
            Log.d("OthersDownloadsWorker", "No files found in Drive downloads folder")
            return@withContext
        }

        Log.d("OthersDownloadsWorker", "Found ${driveFiles.size} files in Drive downloads folder")

        // Download all files (any type). A failed download fails the worker so
        // WorkManager can retry it; already completed files are skipped on retry.
        // Cap concurrent Drive requests to stay clear of API rate limits.
        val downloadSemaphore = Semaphore(MAX_CONCURRENT_TRANSFERS)
        coroutineScope {
            driveFiles.map { driveFile ->
                async {
                    downloadSemaphore.withPermit {
                        downloadFile(drive, driveFile.id, driveFile.name, downloadsDir)
                    }
                }
            }.awaitAll()
        }

        Log.d("OthersDownloadsWorker", "All download files completed")
    }

    private suspend fun downloadFile(
        driveService: Drive,
        fileId: String,
        fileName: String,
        localDir: File
    ) = withContext(Dispatchers.IO) {
        val localFile = File(localDir, fileName)

        if (localFile.exists()) {
            Log.d("OthersDownloadsWorker", "File already exists, skipping: $fileName")
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

            Log.d("OthersDownloadsWorker", "Downloaded: $fileName to ${localFile.absolutePath}")
        } catch (e: Exception) {
            partialFile.delete()
            throw e
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val LIST_PAGE_SIZE = 200
        const val MAX_CONCURRENT_TRANSFERS = 3
    }
}