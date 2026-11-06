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

    private suspend fun copyPdf(src: Uri, destinationPath: String) {
        val destination = File(destinationPath)

        withContext(Dispatchers.IO) {
            if (destination.isFile && destination.length() > 0L) return@withContext

            val parent = destination.absoluteFile.parentFile
                ?: error("PDF destination has no parent directory")
            if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
                error("Unable to create PDF destination directory")
            }

            val temporary = File(parent, destination.name + "." + id + ".tmp")
            try {
                val input = applicationContext.contentResolver.openInputStream(src)
                    ?: error("ContentResolver returned null InputStream for $src")
                input.use { source ->
                    FileOutputStream(temporary).use { output ->
                        source.copyTo(output)
                        output.fd.sync()
                    }
                }
                if (!temporary.isFile || temporary.length() == 0L) {
                    error("Scanned PDF is empty")
                }
                if (destination.exists() && !destination.delete()) {
                    error("Unable to replace incomplete PDF destination")
                }
                if (!temporary.renameTo(destination)) {
                    error("Unable to finalize scanned PDF")
                }
            } finally {
                if (temporary.exists()) temporary.delete()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val KEY_CONTENT_URI      = "CONTENT_URI"
        const val KEY_DESTINATION_PATH = "DESTINATION_PATH"
        const val KEY_OUTPUT_PATH      = "OUTPUT_PATH"
        const val KEY_ERROR            = "ERROR_MESSAGE"

        const val WORK_NAME = "save_ocr_pdf_"

        fun buildDestinationPath(context: Context, fileName: String): String {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir   // fallback to internal storage if SD unavailable
            return File(dir, fileName).absolutePath
        }
    }
}