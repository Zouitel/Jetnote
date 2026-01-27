package zouitel.neighborhood.account

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.usecase.SettingsUseCase
import zouitel.neighborhood.domain.usecase.TagUseCase
import zouitel.neighborhood.ui.JsonInstant
import zouitel.neighborhood.ui.Settings
import zouitel.neighborhood.ui.SettingsMapper
import zouitel.neighborhood.tag.mapper.NoteAndTagMapper
import zouitel.neighborhood.tag.mapper.TagMapper
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.workspace.mapper.DataMapper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.api.services.drive.model.File as DriveFile
import java.io.File as JavaFile

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
): CoroutineWorker(context, params) {
    private val workerScope = CoroutineScope(ioDispatcher)
    // TODO: make this line statable!!
    private val _auth = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid)
    val auth: StateFlow<String?> = _auth

    override suspend fun doWork(): Result {
        return try {
            uploadsData(applicationContext, initializeDriveService())
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private val settings = MutableStateFlow(Settings())
    private val notes = MutableStateFlow<List<Data>>(emptyList())
    private val labels = MutableStateFlow<List<Tag>>(emptyList())
    private val noteAndTags = MutableStateFlow<List<NoteAndTag>>(emptyList())

    init {
        workerScope.launch {
            observeSettings().collect {
                settings.value = settingsMapper.fromDomain(it)
            }
        }
        workerScope.launch {
            getNotes().let {
                notes.value = it.map { note ->
                    dataMapper.fromDomain(note.data)
                }
            }
        }
        workerScope.launch {
            getLabels().let {
                labels.value = labelMapper.fromDomain(it)
            }
        }
        workerScope.launch {
            getNoteAndTags().let {
                noteAndTags.value = noteAndTagsMapper.fromDomain(it)
            }
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
    suspend fun uploadsData(context: Context, driveService: Drive?) = withContext(ioDispatcher) {
        if (driveService == null) return@withContext

        val dataFolderId = findFolderId(driveService, "jetnote_data")
        if (dataFolderId == null) {
            Log.e("SyncScreenModel", "Root folder 'Jetnote_data' not found in Drive.")
            return@withContext
        }
        val uploadJobs = mutableListOf<Deferred<Unit>>()

        // === Upload Settings as JSON ===
        dataFolderId.let {
            uploadJobs.add(async { uploadSettingsAsJson(context, driveService, it) })
        }

        // === Upload Notes as JSON ===
        dataFolderId.let {
            uploadJobs.add(async { uploadNotesAsJson(context, driveService, it) })
        }

        // === Upload Labels as JSON ===
        dataFolderId.let {
            uploadJobs.add(async { uploadLabelsAsJson(context, driveService, it) })
        }

        // === Upload Labels as JSON ===
        dataFolderId.let {
            uploadJobs.add(async { uploadNoteAndTagsAsJson(context, driveService, it) })
        }
    }

    /**
     * Convert Settings object to JSON and upload to Google Drive
     */
    private suspend fun uploadSettingsAsJson(
        context: Context,
        driveService: Drive,
        folderId: String
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                // Convert settings to JSON string
                val settingsJson = JsonInstant.encodeToString(settings.value)

                // Create a temporary file to store the JSON
                val tempFile = JavaFile(context.cacheDir, "settings.json")
                tempFile.writeText(settingsJson)

                // Upload the file
                updateFile(driveService, tempFile, folderId)

                // Clean up the temporary file
                tempFile.delete()

                Log.d("DataUploadWorker", "Settings JSON uploaded successfully")
            }.onFailure {
                error("Failed to upload settings JSON: ${it.message}")
            }
        }
    }

    /**
     * Convert Notes list to JSON and upload to Google Drive
     */
    private suspend fun uploadNotesAsJson(
        context: Context,
        driveService: Drive,
        folderId: String
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                // Convert allNotes to JSON string
                val notesJson = JsonInstant.encodeToString(notes.value)

                // Create a temporary file to store the JSON
                val tempFile = JavaFile(context.cacheDir, "notes.json")
                tempFile.writeText(notesJson)

                // Upload the file
                updateFile(driveService, tempFile, folderId)

                // Clean up the temporary file
                tempFile.delete()

                Log.d("DataUploadWorker", "Notes JSON uploaded successfully")
            }.onFailure {
                error("Failed to upload allNotes JSON: ${it.message}")
            }
        }
    }

    private suspend fun uploadLabelsAsJson(
        context: Context,
        driveService: Drive,
        folderId: String
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                // Convert allNotes to JSON string
                val labelsJson = JsonInstant.encodeToString(labels.value)

                // Create a temporary file to store the JSON
                val tempFile = JavaFile(context.cacheDir, "labels.json")
                tempFile.writeText(labelsJson)

                // Upload the file
                updateFile(driveService, tempFile, folderId)

                // Clean up the temporary file
                tempFile.delete()

                Log.d("DataUploadWorker", "Labels JSON uploaded successfully")
            }.onFailure {
                error("Failed to upload labels JSON: ${it.message}")
            }
        }
    }

    private suspend fun uploadNoteAndTagsAsJson(
        context: Context,
        driveService: Drive,
        folderId: String
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                // Convert allNotes to JSON string
                val noteAndTagsJson = JsonInstant.encodeToString(noteAndTags.value)

                // Create a temporary file to store the JSON
                val tempFile = JavaFile(context.cacheDir, "noteandtags.json")
                tempFile.writeText(noteAndTagsJson)

                // Upload the file
                updateFile(driveService, tempFile, folderId)

                // Clean up the temporary file
                tempFile.delete()

                Log.d("DataUploadWorker", "Labels JSON uploaded successfully")
            }.onFailure {
                error("Failed to upload noteAndTags JSON: ${it.message}")
            }
        }
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
            val mediaContent = FileContent("application/json", localFile)

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