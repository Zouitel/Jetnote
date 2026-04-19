package zouitel.neighborhood.workspace.ui.features

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Copies a temporary ML Kit PDF URI into the app's private Documents directory.
 *
 * Input  keys : CONTENT_URI      — the content:// URI produced by GmsDocumentScanning
 *               DESTINATION_PATH — absolute path of the target .pdf file
 *
 * Output keys : OUTPUT_PATH      — the same destination path, echoed back on success
 *               ERROR_MESSAGE    — human-readable reason on failure
 */
class SavePdfWorker(
    context: Context,
    params : WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uriString       = inputData.getString(KEY_CONTENT_URI)
            ?: return Result.failure(workDataOf(KEY_ERROR to "No source URI supplied"))
        val destinationPath = inputData.getString(KEY_DESTINATION_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR to "No destination path supplied"))

        return try {
            copyPdf(uriString.toUri(), destinationPath)
            Result.success(workDataOf(KEY_OUTPUT_PATH to destinationPath))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf(KEY_ERROR to (e.localizedMessage ?: "Unknown error")))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun copyPdf(src: Uri, destinationPath: String) {
        val destFile = File(destinationPath)

        // Idempotent — skip if the file already exists (e.g. worker was retried).
        if (destFile.exists()) return

        withContext(Dispatchers.IO) {
            destFile.parentFile?.takeIf { !it.exists() }?.mkdirs()

            applicationContext.contentResolver.openInputStream(src)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("ContentResolver returned null InputStream for $src")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val KEY_CONTENT_URI      = "CONTENT_URI"
        const val KEY_DESTINATION_PATH = "DESTINATION_PATH"
        const val KEY_OUTPUT_PATH      = "OUTPUT_PATH"
        const val KEY_ERROR            = "ERROR_MESSAGE"

        /** Unique work-name prefix so multiple scans can queue without collision. */
        const val WORK_NAME = "save_ocr_pdf_"

        /**
         * Builds the app-private destination path for a scanned PDF.
         *
         * Result: <externalFilesDir>/Documents/<fileName>
         */
        fun buildDestinationPath(context: Context, fileName: String): String {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir   // fallback to internal storage if SD unavailable
            return File(dir, fileName).absolutePath
        }
    }
}