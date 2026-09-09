package zouitel.neighborhood.workspace.worker

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zouitel.neighborhood.workspace.ui.utils.getImageDisplayName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CopyMediaWorker(
    context: Context,
    params : WorkerParameters,
    private val ioDeprecated: CoroutineDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val mediaUri = inputData.getString("CONTENT_URI") ?: return Result.failure()
            val destinationPath = inputData.getString("DESTINATION_PATH")

            if (destinationPath != null) {
                copyMediaToDestination(mediaUri.toUri(), destinationPath)
            } else {
                copyMediaToExternalStorage(mediaUri.toUri())
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    // ── Targeted copy (preferred path — used by handleContentReceiver) ────────
    @WorkerThread
    private suspend fun copyMediaToDestination(uri: Uri, destinationPath: String) {
        val destinationFile = File(destinationPath)

        if (destinationFile.exists()) return

        withContext(ioDeprecated) {
            destinationFile.parentFile?.takeIf { !it.exists() }?.mkdirs()

            runCatching {
                applicationContext.contentResolver.openInputStream(uri)?.use { src ->
                    FileOutputStream(destinationFile).use { dst -> src.copyTo(dst) }
                }
            }.onFailure { it.printStackTrace(); throw it }
        }
    }

    // ── Fallback copy (legacy path — no explicit destination supplied) ─────────
    @WorkerThread
    private suspend fun copyMediaToExternalStorage(uri: Uri) {
        val context = applicationContext
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return

        val inputStream = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { it.moveToFirst() }
            FileInputStream(fileDescriptor.fileDescriptor)
        } catch (e: Exception) {
            fileDescriptor.close()
            return
        }

        val mediaName = getImageDisplayName(context, uri)
        val mediaType = context.contentResolver.getType(uri)

        // ── Route to the correct directory based on MIME type ─────────────────
        val mediaDir = when {
            mediaType?.startsWith("image/") == true ->
                Environment.DIRECTORY_PICTURES
            mediaType?.startsWith("video/") == true ->
                Environment.DIRECTORY_MOVIES
            else ->
                // Documents (pdf, docx, xlsx, pptx …) go to DIRECTORY_DOCUMENTS,
                // not DIRECTORY_DOWNLOADS as before.
                Environment.DIRECTORY_DOCUMENTS
        }.let { context.getExternalFilesDir(it) }

        val file = File(mediaDir, mediaName)

        withContext(ioDeprecated) {
            if (mediaDir?.exists() == false) mediaDir.mkdirs()

            if (!file.exists()) {
                FileOutputStream(file).use { dst -> inputStream.copyTo(dst) }
            }
        }

        runCatching {
            inputStream.close()
            fileDescriptor.close()
        }
    }
}