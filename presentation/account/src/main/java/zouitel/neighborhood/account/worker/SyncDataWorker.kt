package zouitel.neighborhood.account.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import zouitel.neighborhood.domain.usecase.TagUseCase
import zouitel.neighborhood.tag.mapper.NoteAndTagMapper
import zouitel.neighborhood.tag.mapper.TagMapper
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.workspace.mapper.DataMapper
import zouitel.neighborhood.workspace.model.Data
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SyncDataWorker(
    context: Context,
    params: WorkerParameters,
    private val insertData: DataUseCase.InsertAll,
    private val insertNoteAndTags: NoteAndTagUseCase.InsertAll,
    private val insertTags: TagUseCase.InsertAll,
    private val dataMapper: DataMapper,
    private val noteAndTagMapper: NoteAndTagMapper,
    private val tagMapper: TagMapper,
    private val ioDispatcher: CoroutineDispatcher
): CoroutineWorker(context, params) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData)) {
            Log.w("SyncDataWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            syncDatabaseFromDrive()
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncDataWorker", "Sync failed: ${e.message}", e)
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
            Log.e("SyncDataWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    private suspend fun syncDatabaseFromDrive() = withContext(ioDispatcher) {
        val driveService = initializeDriveService()
        if (driveService == null) {
            Log.e("SyncDataWorker", "Drive service is null")
            return@withContext
        }

        // Find root folder
        val rootFolderId = findFolderId(driveService, "jetnote_data")
        if (rootFolderId == null) {
            Log.e("SyncDataWorker", "Root folder 'jetnote_data' not found in Drive")
            return@withContext
        }

        // Create temp directory for downloads
        val tempDir = File(applicationContext.cacheDir, "jetnote_sync").apply {
            if (!exists()) mkdirs()
        }

        runCatching {
            // Download all JSON files from Drive
            val downloadJobs = listOf(
                async { downloadAndParse(driveService, "notes.json", rootFolderId, tempDir) { content ->
                    val notes = json.decodeFromString<List<Data>>(content)
                    insertData(dataMapper.toDomain(notes))
                }},
                async { downloadAndParse(driveService, "noteandtags.json", rootFolderId, tempDir) { content ->
                    val noteTags = json.decodeFromString<List<NoteAndTag>>(content)
                    insertNoteAndTags(noteAndTagMapper.toDomain(noteTags))
                }},
                async { downloadAndParse(driveService, "labels.json", rootFolderId, tempDir) { content ->
                    val tags = json.decodeFromString<List<Tag>>(content)
                    insertTags(tagMapper.toDomain(tags))
                }}
            )

            // Wait for all downloads and parsing to complete
            downloadJobs.awaitAll()
            Log.d("SyncDataWorker", "Database sync completed successfully")

        }.onFailure {
            tempDir.deleteRecursively()
            it.printStackTrace()
        }
    }

    private suspend fun downloadAndParse(
        driveService: Drive,
        fileName: String,
        folderId: String,
        tempDir: File,
        onParse: suspend (String) -> Unit
    ) = withContext(ioDispatcher) {
        runCatching {
            // Find file in Drive
            val query = "name='$fileName' and '$folderId' in parents and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val driveFile = result.files?.firstOrNull()
            if (driveFile == null) {
                Log.w("SyncDataWorker", "File not found in Drive: $fileName")
                return@withContext
            }

            // Download to temp file
            val localFile = File(tempDir, fileName)
            FileOutputStream(localFile).use { outputStream ->
                driveService.files().get(driveFile.id).executeMediaAndDownloadTo(outputStream)
                outputStream.flush()
            }

            Log.d("SyncDataWorker", "Downloaded: $fileName")

            // Read and parse JSON
            val content = localFile.readText()
            onParse(content)

        }.onFailure {
            when(it) {
                is IOException -> Log.e("SyncDataWorker", "Error downloading $fileName: ${it.message}", it)
                is Exception -> Log.e("SyncDataWorker", "Error parsing $fileName: ${it.message}", it)
            }
        }
    }

    private suspend fun findFolderId(
        driveService: Drive,
        folderName: String,
        parentId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
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
        }.onFailure {
            it.printStackTrace()
            null
        }.getOrNull()
    }
}