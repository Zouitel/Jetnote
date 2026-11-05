package zouitel.neighborhood.account.worker

import android.content.Context
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
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.utils.findFolderId
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.usecase.SettingsUseCase
import zouitel.neighborhood.domain.usecase.TagUseCase
import zouitel.neighborhood.tag.mapper.NoteAndTagMapper
import zouitel.neighborhood.tag.mapper.TagMapper
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.ui.JsonInstant
import zouitel.neighborhood.ui.Settings
import zouitel.neighborhood.ui.SettingsMapper
import zouitel.neighborhood.workspace.mapper.DataMapper
import zouitel.neighborhood.workspace.model.Data
import java.io.File as JavaFile
import java.io.IOException

class DataUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val observeSettings: SettingsUseCase.ObserveSettings,
    private val settingsMapper: SettingsMapper,
    private val getNotes: NoteUseCase.GetAll,
    private val dataMapper: DataMapper,
    private val getLabels: TagUseCase.GetAll,
    private val labelMapper: TagMapper,
    private val getNoteAndTags: NoteAndTagUseCase.GetAll,
    private val noteAndTagsMapper: NoteAndTagMapper,
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!AccountWorkIdentity.belongsToCurrentUser(inputData)) {
            Log.w("DataUploadWorker", "Skipping work because its Firebase account is missing or no longer active")
            return Result.failure()
        }

        return try {
            // Read each source inside this work execution. No background collectors can
            // race the upload and leave an empty/default snapshot behind.
            val settingsSnapshot = settingsMapper.fromDomain(observeSettings().first())
            val notesSnapshot = getNotes().map { note ->
                dataMapper.fromDomain(note.data)
            }
            val labelsSnapshot = labelMapper.fromDomain(getLabels())
            val noteAndTagsSnapshot = noteAndTagsMapper.fromDomain(getNoteAndTags())

            val driveService = initializeDriveService()
                ?: throw IOException("Google Drive account is not available")

            uploadsData(
                context = applicationContext,
                driveService = driveService,
                settings = settingsSnapshot,
                notes = notesSnapshot,
                labels = labelsSnapshot,
                noteAndTags = noteAndTagsSnapshot
            )

            Result.success()

        } catch (cancelled: CancellationException) {
        throw cancelled
        } catch (error: Exception) {
        Log.e("DataUploadWorker", "Data upload failed: ${error.message}", error)
        if (error is IOException) Result.retry() else Result.failure()
        }
    }

    private suspend fun initializeDriveService(): Drive? = withContext(ioDispatcher) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
                ?: return@withContext null

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = account.account
            }

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Jetnote").build()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("DataUploadWorker", "Failed to initialize Drive service", error)
            null
        }
    }

    @WorkerThread
    private suspend fun uploadsData(
        context: Context,
        driveService: Drive,
        settings: Settings,
        notes: List<Data>,
        labels: List<Tag>,
        noteAndTags: List<NoteAndTag>
    ) = withContext(ioDispatcher) {
        val dataFolderId = findFolderId(driveService, "jetnote_data")
            ?: throw IOException("Google Drive folder 'jetnote_data' was not found")

        coroutineScope {
            listOf(
                async { uploadSettingsAsJson(context, driveService, dataFolderId, settings) },
                async { uploadNotesAsJson(context, driveService, dataFolderId, notes) },
                async { uploadLabelsAsJson(context, driveService, dataFolderId, labels) },
                async { uploadNoteAndTagsAsJson(context, driveService, dataFolderId, noteAndTags) }
            ).awaitAll()
        }
    }

    private suspend fun uploadSettingsAsJson(
        context: Context,
        driveService: Drive,
        folderId: String,
        settings: Settings
    ) {
        uploadJson(context, driveService, folderId, "settings.json", settings)
    }

    private suspend fun uploadNotesAsJson(
        context: Context,
        driveService: Drive,
        folderId: String,
        notes: List<Data>
    ) {
        uploadJson(context, driveService, folderId, "notes.json", notes)
    }

    private suspend fun uploadLabelsAsJson(
        context: Context,
        driveService: Drive,
        folderId: String,
        labels: List<Tag>
    ) {
        uploadJson(context, driveService, folderId, "labels.json", labels)
    }

    private suspend fun uploadNoteAndTagsAsJson(
        context: Context,
        driveService: Drive,
        folderId: String,
        noteAndTags: List<NoteAndTag>
    ) {
        uploadJson(context, driveService, folderId, "noteandtags.json", noteAndTags)
    }

    private suspend inline fun <reified T> uploadJson(
        context: Context,
        driveService: Drive,
        folderId: String,
        fileName: String,
        value: T
    ) {
        withContext(ioDispatcher) {
            val tempFile = JavaFile(context.cacheDir, fileName)
            try {
                tempFile.writeText(JsonInstant.encodeToString(value))
                updateFile(driveService, tempFile, folderId)
                Log.d("DataUploadWorker", "$fileName uploaded successfully")
            } finally {
                tempFile.delete()
            }
        }
    }

    private suspend fun updateFile(
        driveService: Drive,
        localFile: JavaFile,
        folderId: String
    ) = withContext(ioDispatcher) {
        if (!localFile.exists()) {
            throw IOException("File not found for upload: ${localFile.absolutePath}")
        }

        val query = "name='${localFile.name}' and '$folderId' in parents and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        val existingFileId = result.files?.firstOrNull()?.id
        val mediaContent = FileContent("application/json", localFile)

        if (existingFileId != null) {
            driveService.files().update(existingFileId, null, mediaContent).execute()
            Log.d("DataUploadWorker", "File updated: ${localFile.name}")
        } else {
            val fileMetadata = DriveFile().apply {
                name = localFile.name
                parents = listOf(folderId)
            }
            driveService.files().create(fileMetadata, mediaContent).execute()
            Log.d("DataUploadWorker", "File created: ${localFile.name}")
        }
    }
}
