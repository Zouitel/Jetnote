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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile

class UploadsDataWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("UploadsDataWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            uploadsData(applicationContext, initializeDriveService())
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
                // Build the Drive service
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
    suspend fun uploadsData(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        if (driveService == null) return@withContext

        val rootFolderName = "Jetnote"
        val rootFolderId = findFolderId(driveService, rootFolderName)

        if (rootFolderId == null) {
            Log.e("SyncScreenModel", "Root folder 'Jetnote' not found in Drive.")
            return@withContext
        }

        // A list to hold all the asynchronous upload jobs
        val uploadJobs = mutableListOf<Deferred<Unit>>()

        // === Upload Database Files Concurrently ===
        val databaseFolderName = "database"
        val databaseFolderId = findFolderId(driveService, databaseFolderName, rootFolderId)
        databaseFolderId?.let { dbFolderId ->
            val databasePath =
                context.getDatabasePath("jetnote_database")?.parentFile?.canonicalPath
            databasePath?.let {
                uploadJobs.add(async {
                    updateFile(
                        driveService,
                        JavaFile("$it/jetnote_database"),
                        dbFolderId
                    )
                })
                uploadJobs.add(async {
                    updateFile(
                        driveService,
                        JavaFile("$it/jetnote_database-shm"),
                        dbFolderId
                    )
                })
                uploadJobs.add(async {
                    updateFile(
                        driveService,
                        JavaFile("$it/jetnote_database-wal"),
                        dbFolderId
                    )
                })
            }
        }

        // === Upload Datastore File Concurrently ===
        val datastoreFolderName = "datastore"
        val datastoreFolderId = findFolderId(driveService, datastoreFolderName, rootFolderId)
        datastoreFolderId?.let { dsFolderId ->
            val datastoreFile =
                JavaFile(context.filesDir.resolve("datastore"), "jetnote_datastore.preferences_pb")
            if (datastoreFile.exists()) {
                uploadJobs.add(async { updateFile(driveService, datastoreFile, dsFolderId) })
            } else {
                Log.e("SyncScreenModel", "Datastore file not found: ${datastoreFile.absolutePath}")
            }
        }

        // === Upload Media Files Concurrently ===
        val mediaFolderName = "medias"
        val mediaFolderId = findFolderId(driveService, mediaFolderName, rootFolderId)
        mediaFolderId?.let { mFolderId ->
            val mediaDir = Environment.DIRECTORY_DOWNLOADS.let { context.getExternalFilesDir(it) }
            if (mediaDir?.exists() == true && mediaDir.isDirectory) {
                mediaDir.listFiles()?.forEach { localMediaFile ->
                    if (localMediaFile.isFile) {
                        uploadJobs.add(async {
                            // Your existing logic to check and then upload the file
                            val query =
                                "name='${localMediaFile.name}' and '$mFolderId' in parents and trashed=false"
                            val result = driveService.files().list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .execute()

                            if (result.files.isNullOrEmpty()) {
                                updateFile(driveService, localMediaFile, mFolderId)
                            }
                        })
                    }
                }
            } else {
                Log.e("UploadsDataWorker", "Media directory not found: ${mediaDir?.absolutePath}")
            }
        }

        // Wait for all upload jobs to complete
        uploadJobs.awaitAll()
        Log.d("UploadsDataWorker", "All uploads completed concurrently.")
    }

    private suspend fun updateFile(driveService: Drive, localFile: JavaFile, folderId: String) {
        if (!localFile.exists()) {
            Log.e("SyncScreenModel", "File not found for upload: ${localFile.absolutePath}")
            return
        }
        withContext(Dispatchers.IO) {
            // Check if the file already exists in Google Drive
            val query = "name='${localFile.name}' and '$folderId' in parents and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            val existingFileId = result.files?.firstOrNull()?.id
            val mediaContent = FileContent("application/octet-stream", localFile)

            if (existingFileId != null) {
                // File exists, update it
                driveService.files().update(existingFileId, null, mediaContent).execute()
                Log.d("SyncScreenModel", "File updated: ${localFile.name}")
            } else {
                // File does not exist, create it
                val fileMetadata = DriveFile().apply {
                    name = localFile.name
                    parents = listOf(folderId)
                }
                driveService.files().create(fileMetadata, mediaContent).execute()
                Log.d("SyncScreenModel", "File created: ${localFile.name}")
            }
        }
    }
}