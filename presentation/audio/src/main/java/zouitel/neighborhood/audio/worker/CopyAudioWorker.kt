package zouitel.neighborhood.audio.worker

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zouitel.neighborhood.audio.utils.AudioStorage
import zouitel.neighborhood.audio.utils.Constants.INPUT_ID
import zouitel.neighborhood.audio.utils.Constants.INPUT_TITLE
import zouitel.neighborhood.audio.utils.Constants.INPUT_URI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CopyAudioWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val audioId = inputData.getLong(INPUT_ID, INVALID_AUDIO_ID)
        val title = inputData.getString(INPUT_TITLE)
        val source = inputData.getString(INPUT_URI)

        if (audioId == INVALID_AUDIO_ID || title.isNullOrBlank() || source.isNullOrBlank()) {
            return@withContext Result.failure()
        }

        try {
            copyAudioToAppStorage(audioId, title, Uri.parse(source))
            Result.success()
        } catch (error: IOException) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    @WorkerThread
    private suspend fun copyAudioToAppStorage(audioId: Long, title: String, uri: Uri) {
        val directory = AudioStorage.directory(applicationContext)

        if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
            throw IOException("Unable to create app audio storage directory")
        }

        val destination = AudioStorage.destination(directory, audioId, title)
        if (destination.isFile && destination.length() > 0L) return

        val temporary = File(directory, "${destination.name}.partial")
        try {
            val input = applicationContext.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open source audio: $uri")

            input.use { source ->
                withContext(Dispatchers.IO) {
                    FileOutputStream(temporary).use { output ->
                        source.copyTo(output)
                        output.fd.sync()
                    }
                }
            }

            if (!temporary.isFile || temporary.length() == 0L) {
                throw IOException("Audio copy produced an empty file")
            }

            if (!temporary.renameTo(destination)) {
                throw IOException("Unable to finalize copied audio file")
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private companion object {
        const val INVALID_AUDIO_ID = Long.MIN_VALUE
        const val MAX_RETRIES = 3
    }
}
