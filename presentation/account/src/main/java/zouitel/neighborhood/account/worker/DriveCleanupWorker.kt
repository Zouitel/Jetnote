package zouitel.neighborhood.account.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.mapper.AudioMapper
import zouitel.neighborhood.account.mapper.MediaMapper
import zouitel.neighborhood.domain.usecase.NoteUseCase

class DriveCleanupWorker(
    context: Context,
    params: WorkerParameters,
    private val notes: NoteUseCase.ObserveAll,
    private val ioDispatcher: CoroutineDispatcher,
    private val mediaMapper: MediaMapper,
    private val audioMapper: AudioMapper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        return@withContext try {
            val driveService = initializeDriveService() ?: return@withContext Result.failure()

            // 1. Fetch all current notes from the local database
            val allNotes = notes().first()

            // 2. Extract valid file NAMES (not full paths) from the database
            val validMediaNames = mediaMapper.fromDomain(
                allNotes.map { it.data.medias }
            ).map { uriString ->
                // Extract just the file name from the URI/Path (e.g., "IMG_123.jpg")
                uriString.uri.substringAfterLast('/')
            }.toSet()

            val validAudioNames = audioMapper.fromDomain(
                allNotes.map { it.data.audios }
            ).map { uriString ->
                // Extract just the file name from the URI/Path (e.g., "IMG_123.jpg")
                uriString.uri.substringAfterLast('/')
            }.toSet()

            // 3. Run Drive Cleanup
            cleanupDriveOrphans(driveService, "pictures", validMediaNames)
            cleanupDriveOrphans(driveService, "movies", validMediaNames)
            cleanupDriveOrphans(driveService, "audios", validAudioNames)

            Result.success()
        } catch (e: Exception) {
            Log.e("DriveCleanupWorker", "Failed to clean up Drive data: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun cleanupDriveOrphans(
        driveService: Drive,
        targetFolderName: String,
        validFileNames: Set<String>
    ) = withContext(Dispatchers.IO) {
        try {
            // Find root media folder
            val mediasFolderId = findFolderId(driveService, "jetnote_medias")
            if (mediasFolderId == null) {
                Log.d(
                    "DriveCleanupWorker",
                    "Root folder 'jetnote_medias' not found. Nothing to clean."
                )
                return@withContext
            }

            // Find target subfolder (e.g., "pictures")
            val targetFolderId = findFolderId(driveService, targetFolderName, mediasFolderId)
            if (targetFolderId == null) {
                Log.d(
                    "DriveCleanupWorker",
                    "Target folder '$targetFolderName' not found. Nothing to clean."
                )
                return@withContext
            }

            // Get all files from this Drive folder
            val query = "'$targetFolderId' in parents and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val driveFiles = result.files ?: emptyList()

            if (driveFiles.isEmpty()) {
                Log.d("DriveCleanupWorker", "No files found in Drive folder '$targetFolderName'")
                return@withContext
            }

            // Compare and Delete
            var deletedCount = 0
            driveFiles.forEach { driveFile ->
                if (!validFileNames.contains(driveFile.name)) {
                    Log.d(
                        "DriveCleanupWorker",
                        "Deleting orphaned file from Drive: ${driveFile.name}"
                    )
                    driveService.files().delete(driveFile.id).execute()
                    deletedCount++
                }
            }

            Log.d(
                "DriveCleanupWorker",
                "Drive cleanup complete for '$targetFolderName'. Deleted $deletedCount orphans."
            )

        } catch (e: Exception) {
            Log.e(
                "DriveCleanupWorker",
                "Error cleaning up Drive folder $targetFolderName: ${e.message}",
                e
            )
        }
    }

    private suspend fun initializeDriveService(): Drive? = withContext(ioDispatcher) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf(DriveScopes.DRIVE_FILE) // Ensure you have permission to delete files you created
            )

            val lastAccount = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (lastAccount == null) return@withContext null

            credential.selectedAccount = lastAccount.account

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Drive File Manager").build()
        } catch (e: Exception) {
            Log.e("DriveCleanupWorker", "Drive init failed: ${e.message}", e)
            null
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
                .setFields("files(id)")
                .execute()

            result.files?.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
}