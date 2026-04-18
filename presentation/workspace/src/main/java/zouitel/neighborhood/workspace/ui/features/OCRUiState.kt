package zouitel.neighborhood.workspace.ui.features

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── State ────────────────────────────────────────────────────────────────────

data class OCRUiState(
    val scannedPages: List<Uri> = emptyList(),
    val pdfUri: Uri? = null,
    val pageCount: Int = 0,
    val isSaving: Boolean = false,
    val savedToNote: Boolean = false,
    val error: String? = null,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class OCRScreenModel(private val application: Application) : ScreenModel {

    private val _uiState = MutableStateFlow(OCRUiState())
    val uiState: StateFlow<OCRUiState> = _uiState.asStateFlow()

    /**
     * Build the ML Kit scanner options.
     * - SCANNER_MODE_FULL → full UI with auto-capture, crop handles, filters
     * - galleryImportAllowed → user can also pick from gallery
     * - pageLimit(0) → unlimited pages (set e.g. 10 to cap)
     */
    fun buildScannerOptions(): GmsDocumentScannerOptions =
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

    /**
     * Returns the ML Kit scanner client — pass this to
     * getStartScanIntent() inside the composable.
     */
    fun getScannerClient() =
        GmsDocumentScanning.getClient(buildScannerOptions())

    /**
     * Called after a successful scan result from the launcher.
     */
    fun onScanResult(result: GmsDocumentScanningResult) {
        val pages = result.pages?.map { it.imageUri } ?: emptyList()
        val pdf = result.pdf?.uri
        _uiState.update {
            it.copy(
                scannedPages = pages,
                pdfUri = pdf,
                pageCount = pages.size,
                error = null,
            )
        }
    }

    fun onScanError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Attach scanned pages to the given note.
     * Replace this body with your own repository call.
     *
     * @param noteId   ID of the note to attach pages to (-1 = create new note)
     * @param title    Auto-generated title if creating a new note
     */
    fun saveToNote(noteId: String = "", title: String = generateTitle()) {
        screenModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // ── TODO: replace with your NoteRepository call ──────────────
                // Example:
                //   if (noteId == -1L) {
                //       noteRepository.createNoteWithScans(
                //           title = title,
                //           imageUris = uiState.value.scannedPages,
                //           pdfUri = uiState.value.pdfUri
                //       )
                //   } else {
                //       noteRepository.attachScansToNote(
                //           noteId = noteId,
                //           imageUris = uiState.value.scannedPages,
                //           pdfUri = uiState.value.pdfUri
                //       )
                //   }
                // ─────────────────────────────────────────────────────────────

                _uiState.update { it.copy(isSaving = false, savedToNote = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Copy the PDF to the app's files directory for persistent storage.
     * ML Kit's result URIs are temporary — call this before the activity finishes.
     */
    fun persistPdf(destinationDir: File): Uri? {
        val srcUri = _uiState.value.pdfUri ?: return null
        return try {
            val destFile = File(destinationDir, generateTitle() + ".pdf")
            application.contentResolver
                .openInputStream(srcUri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Could not save PDF: ${e.localizedMessage}") }
            null
        }
    }

    fun resetScan() {
        _uiState.update { OCRUiState() }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun generateTitle(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return "Scan ${fmt.format(Date())}"
    }
}