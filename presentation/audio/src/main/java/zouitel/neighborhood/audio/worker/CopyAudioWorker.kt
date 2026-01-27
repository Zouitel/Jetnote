package zouitel.neighborhood.audio.worker

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.core.net.toUri
import zouitel.neighborhood.audio.utils.Constants.INPUT_TITLE
import zouitel.neighborhood.audio.utils.Constants.INPUT_URI

class CopyAudioWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDeprecated: CoroutineDispatcher
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(ioDeprecated) {
        return@withContext try {
            val uri = inputData.getString(INPUT_URI) ?: return@withContext Result.failure()
            val title = inputData.getString(INPUT_TITLE) ?: return@withContext Result.failure()

            copyAudioToExternalStorage(title, uri.toUri())
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }

    }

    @WorkerThread
    private suspend fun copyAudioToExternalStorage(title: String, uri: Uri) {
        val context = applicationContext
        val podcasts = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)?.path ?: return
        val file = File(podcasts, title)

        val contentResolver: ContentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return
        val inputStream = runCatching {
            FileInputStream(fileDescriptor.fileDescriptor)
        }.onFailure {
            fileDescriptor.close()
            return
        }

        withContext(Dispatchers.IO) {
            FileOutputStream(file).use { outputStream ->
                inputStream.getOrNull()?.copyTo(outputStream)
            }
        }

        runCatching {
            inputStream.getOrNull()?.close()
            fileDescriptor.close()
        }
    }
}