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
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import zouitel.neighborhood.account.utils.findFolderId
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
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData, applicationContext)) {
            Log.w("SyncDataWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }
        return try {
            syncDatabaseFromDrive()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Log.e("SyncDataWorker", "Transient error while syncing from Drive (attempt ${runAttemptCount + 1}): ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e("SyncDataWorker", "Sync failed: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun syncDatabaseFromDrive() = withContext(ioDispatcher) {
        val driveService = initializeDriveService(applicationContext, inputData, "SyncDataWorker", ioDispatcher)
            ?: throw IOException("Google Drive account is not available")

        // Find root folder
        val rootFolderId = findFolderId(driveService, "jetnote_data")
            ?: throw IOException("Root folder 'jetnote_data' not found in Drive")

        // Create temp directory for downloads
        val tempDir = File(applicationContext.cacheDir, "jetnote_sync").apply {
            if (!exists()) mkdirs()
        }

        try {
            // Download all JSON files from Drive. Any download or parse failure must
            // propagate: reporting success while the restore silently failed leaves
            // the user convinced their notes are safe when they are not.
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
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun downloadAndParse(
        driveService: Drive,
        fileName: String,
        folderId: String,
        tempDir: File,
        onParse: suspend (String) -> Unit
    ) = withContext(ioDispatcher) {
        // Find file in Drive
        val query = "name='$fileName' and '$folderId' in parents and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val driveFile = result.files?.firstOrNull()
        if (driveFile == null) {
            // Not an error: the folder structure is created at sign-in, but files
            // only exist after the first successful upload.
            Log.w("SyncDataWorker", "File not found in Drive, skipping: $fileName")
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
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}