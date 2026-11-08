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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MoviesDownloadsWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("MoviesDownloadsWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            downloadMovies(applicationContext, initializeDriveService())
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("MoviesDownloadsWorker", "Download failed (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
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
                    AccountBoundDriveRequestInitializer(credential, applicationContext, inputData)
                ).setApplicationName("Drive File Manager").build()
            }
        } catch (e: Exception) {
            Log.e("MoviesDownloadsWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    @WorkerThread
    suspend fun downloadMovies(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        // A missing service or folder must fail the work: returning normally here
        // used to report a successful sync while nothing was downloaded.
        val drive = driveService ?: throw IOException("Drive service is not available")

        val mediasFolderId = findFolderId(drive, "jetnote_medias")
            ?: throw IOException("Root folder 'jetnote_medias' not found in Drive")

        val moviesFolderId = findFolderId(drive, "movies", mediasFolderId)
            ?: throw IOException("Movies folder not found in Drive")

        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: throw IOException("Movies directory not accessible")

        if (!moviesDir.exists()) {
            moviesDir.mkdirs()
        }

        val query = "'$moviesFolderId' in parents and trashed=false"
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
            Log.d("MoviesDownloadsWorker", "No files found in Drive movies folder")
            return@withContext
        }

        Log.d("MoviesDownloadsWorker", "Found ${driveFiles.size} files in Drive movies folder")

        // A failed download fails the worker so WorkManager can retry it;
        // already completed files are skipped on retry.
        // Cap concurrent Drive requests to stay clear of API rate limits.
        val downloadSemaphore = Semaphore(MAX_CONCURRENT_TRANSFERS)
        coroutineScope {
            driveFiles.map { driveFile ->
                async {
                    downloadSemaphore.withPermit {
                        downloadFile(drive, driveFile.id, driveFile.name, moviesDir)
                    }
                }
            }.awaitAll()
        }

        Log.d("MoviesDownloadsWorker", "All download files completed")
    }

    private suspend fun downloadFile(
        driveService: Drive,
        fileId: String,
        fileName: String,
        localDir: File
    ) = withContext(Dispatchers.IO) {
        val localFile = File(localDir, fileName)

        if (localFile.exists()) {
            Log.d("MoviesDownloadsWorker", "File already exists, skipping: $fileName")
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

            Log.d("MoviesDownloadsWorker", "Downloaded: $fileName to ${localFile.absolutePath}")
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