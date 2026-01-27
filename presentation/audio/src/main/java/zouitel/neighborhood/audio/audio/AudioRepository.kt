package zouitel.neighborhood.audio.audio

import android.graphics.Bitmap
import zouitel.neighborhood.audio.model.Audio
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AudioRepository(
    private val localMediaDataSource: LocalMediaDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun loadAudioFiles(query: String): List<Audio> = withContext(ioDispatcher) {
        return@withContext localMediaDataSource.loadAudioFiles(query)
    }

    fun loadAudioThumbnail(filePath: String): Bitmap? {
        return localMediaDataSource.getAudioThumbnail(filePath)
    }
}