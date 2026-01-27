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
    params: WorkerParameters,
    private val ioDeprecated: CoroutineDispatcher
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val mediaUri = inputData.getString("CONTENT_URI") ?: return Result.failure()
            val destinationPath = inputData.getString("DESTINATION_PATH")

            if (destinationPath != null) {
                // New approach: Copy to specific destination
                copyMediaToDestination(mediaUri.toUri(), destinationPath)
            } else {
                // Old approach: Copy to external storage
                copyMediaToExternalStorage(mediaUri.toUri())
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    @WorkerThread
    private suspend fun copyMediaToDestination(uri: Uri, destinationPath: String) {
        val context = applicationContext
        val destinationFile = File(destinationPath)

        // If file already exists, skip copying
        if (destinationFile.exists()) {
            return
        }

        withContext(ioDeprecated) {
            val parentDir = destinationFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    @WorkerThread
    private suspend fun copyMediaToExternalStorage(uri: Uri) {
        val context = applicationContext
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return
        val inputStream = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
            }
            FileInputStream(fileDescriptor.fileDescriptor)
        } catch (e: Exception) {
            fileDescriptor.close()
            return
        }
        val mediaName = getImageDisplayName(context, uri)
        val mediaType = context.contentResolver.getType(uri)

        val mediaPath = when {
            mediaType?.startsWith("image/") == true -> Environment.DIRECTORY_PICTURES?.let { context.getExternalFilesDir(it) }
            mediaType?.startsWith("video/") == true -> Environment.DIRECTORY_MOVIES?.let { context.getExternalFilesDir(it) }
            else -> Environment.DIRECTORY_DOWNLOADS?.let { context.getExternalFilesDir(it) }
        }

        val file = File(mediaPath, mediaName)
        withContext(ioDeprecated) {
            if (!mediaPath?.exists()!!) mediaPath.mkdirs()

            if(file.exists().not()) {
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        runCatching {
            inputStream.close()
            fileDescriptor.close()
        }
    }
}