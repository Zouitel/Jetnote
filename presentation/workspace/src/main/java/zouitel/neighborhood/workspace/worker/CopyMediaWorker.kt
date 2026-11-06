package zouitel.neighborhood.workspace.worker

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import zouitel.neighborhood.workspace.ui.utils.getImageDisplayName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CopyMediaWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDeprecated: CoroutineDispatcher = Dispatchers.IO
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val uri = inputData.getString(KEY_CONTENT_URI)?.toUri()
            ?: return Result.failure(workDataOf(KEY_ERROR to "No source URI supplied"))
        val destinationPath = inputData.getString(KEY_DESTINATION_PATH)
        val destination = if (destinationPath.isNullOrBlank()) {
            copyMediaToExternalStorage(uri)
        } else {
            copyMediaToDestination(uri, destinationPath)
        }
        if (!destination.isFile || destination.length() == 0L) {
            return Result.failure(workDataOf(KEY_ERROR to "Media copy did not produce a valid file"))
        }
        Result.success(workDataOf(KEY_OUTPUT_PATH to destination.absolutePath))
    } catch (error: Exception) {
        Result.failure(workDataOf(KEY_ERROR to (error.localizedMessage ?: "Unable to copy media")))
    }

    @WorkerThread
    private suspend fun copyMediaToDestination(uri: Uri, path: String): File =
        withContext(ioDeprecated) { copyToFile(uri, File(path)) }

    @WorkerThread
    private suspend fun copyMediaToExternalStorage(uri: Uri): File = withContext(ioDeprecated) {
        val context = applicationContext
        val mime = context.contentResolver.getType(uri).orEmpty()
        val directory = when {
            mime.startsWith("image/") -> Environment.DIRECTORY_PICTURES
            mime.startsWith("video/") -> Environment.DIRECTORY_MOVIES
            else -> Environment.DIRECTORY_DOCUMENTS
        }
        val parent = context.getExternalFilesDir(directory) ?: File(context.filesDir, directory)
        val name = getImageDisplayName(context, uri).ifBlank { "attachment.bin" }
        copyToFile(uri, File(parent, name))
    }

    private fun copyToFile(uri: Uri, destination: File): File {
        if (destination.isFile && destination.length() > 0L) return destination
        val parent = destination.absoluteFile.parentFile ?: error("Destination has no parent")
        if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
            error("Unable to create destination directory")
        }
        val temporary = File(parent, destination.name + "." + id + ".tmp")
        try {
            val input = applicationContext.contentResolver.openInputStream(uri)
                ?: error("Unable to open source URI: $uri")
            input.use { source ->
                FileOutputStream(temporary).use { output ->
                    source.copyTo(output)
                    output.fd.sync()
                }
            }
            if (!temporary.isFile || temporary.length() == 0L) error("Copied file is empty")
            if (destination.exists() && !destination.delete()) error("Unable to replace destination")
            if (!temporary.renameTo(destination)) error("Unable to finalize copied file")
            return destination
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    companion object {
        const val KEY_CONTENT_URI = "CONTENT_URI"
        const val KEY_DESTINATION_PATH = "DESTINATION_PATH"
        const val KEY_OUTPUT_PATH = "OUTPUT_PATH"
        const val KEY_ERROR = "ERROR_MESSAGE"
    }
}
