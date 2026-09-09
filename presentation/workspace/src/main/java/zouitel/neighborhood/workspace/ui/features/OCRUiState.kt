package zouitel.neighborhood.workspace.ui.features

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// ─── State ────────────────────────────────────────────────────────────────────

data class OCRUiState(
    val scannedPages: List<Uri> = emptyList(),
    val pdfUri      : Uri?      = null,
    val pageCount   : Int       = 0,
    val isSaving    : Boolean   = false,
    // Null  → not yet saved
    // ""    → work enqueued, path not yet confirmed
    // path  → absolute path of the persisted PDF
    val savedPdfPath: String?   = null,
    val savedToNote : Boolean   = false,
    val error       : String?   = null,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────
@Stable
class OCRScreenModel(private val application: Application) : ScreenModel {

    private val _uiState = MutableStateFlow(OCRUiState())
    val uiState: StateFlow<OCRUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(application)

    // ── Scanner setup ─────────────────────────────────────────────────────────

    /**
     * Build the ML Kit scanner options.
     *  - SCANNER_MODE_FULL  → full UI with auto-capture, crop handles, filters
     *  - galleryImportAllowed → user can also pick from gallery
     *  - pageLimit(20) → cap at 20 pages
     */
    fun buildScannerOptions(): GmsDocumentScannerOptions =
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

    /** Returns the ML Kit scanner client. Pass to getStartScanIntent() inside the composable. */
    fun getScannerClient() = GmsDocumentScanning.getClient(buildScannerOptions())

    // ── Scan result handling ──────────────────────────────────────────────────

    /** Called after a successful scan result from the launcher. */
    fun onScanResult(result: GmsDocumentScanningResult) {
        val pages = result.pages?.map { it.imageUri } ?: emptyList()
        val pdf   = result.pdf?.uri
        _uiState.update {
            it.copy(
                scannedPages = pages,
                pdfUri       = pdf,
                pageCount    = pages.size,
                error        = null,
            )
        }
    }

    fun onScanError(message: String) = _uiState.update { it.copy(error = message) }
    fun clearError()                  = _uiState.update { it.copy(error = null) }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Saves the scanned PDF to the app's private Documents directory using a
     * [SavePdfWorker] running in the background, then optionally attaches it
     * to the note identified by [noteId].
     *
     * State transitions:
     *   isSaving = true  (work enqueued)
     *   isSaving = false + savedPdfPath = <path>  (success)
     *   isSaving = false + error = <msg>           (failure)
     */
    fun saveToNote(noteId: String = "", title: String = generateTitle()) {
        val srcUri = _uiState.value.pdfUri
        if (srcUri == null) {
            _uiState.update { it.copy(error = "No PDF to save — please scan a document first.") }
            return
        }

        val fileName        = "$title.pdf"
        val destinationPath = SavePdfWorker.buildDestinationPath(application, fileName)

        _uiState.update { it.copy(isSaving = true, savedPdfPath = "") }

        // ── Build & enqueue the worker ────────────────────────────────────────
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            SavePdfWorker.KEY_CONTENT_URI      to srcUri.toString(),
            SavePdfWorker.KEY_DESTINATION_PATH to destinationPath,
        )

        val request = OneTimeWorkRequestBuilder<SavePdfWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        // Unique name per scan so concurrent saves don't collide.
        workManager.enqueueUniqueWork(
            "${SavePdfWorker.WORK_NAME}${Random.nextInt()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )

        // ── Observe result ────────────────────────────────────────────────────
        workManager.getWorkInfoByIdLiveData(request.id)
            .observeForever { workInfo ->
                if (workInfo == null) return@observeForever

                when {
                    workInfo.state.isFinished && workInfo.state ==
                            androidx.work.WorkInfo.State.SUCCEEDED -> {
                        val savedPath = workInfo.outputData
                            .getString(SavePdfWorker.KEY_OUTPUT_PATH) ?: destinationPath

                        _uiState.update {
                            it.copy(isSaving = false, savedPdfPath = savedPath)
                        }

                        // ── Attach to note after the file is safely on disk ───
                        attachToNote(noteId = noteId, localPdfPath = savedPath, title = title)
                    }

                    workInfo.state.isFinished -> {   // FAILED or CANCELLED
                        val reason = workInfo.outputData
                            .getString(SavePdfWorker.KEY_ERROR) ?: "Unknown error"
                        _uiState.update {
                            it.copy(isSaving = false, error = "Failed to save PDF: $reason")
                        }
                    }
                    // ENQUEUED / RUNNING → keep isSaving = true
                }
            }
    }

    // ── Note attachment ───────────────────────────────────────────────────────

    /**
     * Called once the PDF is safely stored on disk.
     * The composable's LaunchedEffect already appended the Media entry to
     * [data.value.medias] and popped the back-stack, so all that remains here
     * is flipping [savedToNote] to signal completion (e.g. for analytics /
     * future repository calls).
     *
     * @param noteId       Target note ID; empty = new note.
     * @param localPdfPath Absolute path of the persisted PDF file.
     * @param title        Title used when creating a new note.
     */
    private fun attachToNote(noteId: String, localPdfPath: String, title: String) {
        screenModelScope.launch {
            try {
                // ── TODO: replace with your NoteRepository call ───────────────
                // if (noteId.isBlank()) {
                //     noteRepository.createNoteWithPdf(
                //         title   = title,
                //         pdfPath = localPdfPath,
                //     )
                // } else {
                //     noteRepository.attachPdfToNote(
                //         noteId  = noteId,
                //         pdfPath = localPdfPath,
                //     )
                // }
                // ─────────────────────────────────────────────────────────────
                _uiState.update { it.copy(savedToNote = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Saved PDF but failed to attach to note: ${e.localizedMessage}")
                }
            }
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    fun resetScan() = _uiState.update { OCRUiState() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun generateTitle(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return "Scan ${fmt.format(Date())}"
    }
}