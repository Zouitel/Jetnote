package zouitel.neighborhood.bin.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import zouitel.neighborhood.bin.mapper.AudioMapper
import zouitel.neighborhood.bin.mapper.MediaMapper
import zouitel.neighborhood.domain.usecase.ConversationUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.bin.utils.cleanupOrphanedAudios
import zouitel.neighborhood.bin.utils.cleanupOrphanedImages
import zouitel.neighborhood.bin.utils.cleanupOrphanedVideos

class BinWorker(
    private val context: Context,
    params: WorkerParameters,
    private val cleanupNotes: NoteUseCase.Cleanup,
    private val cleanupConversations: ConversationUseCase.Cleanup,
    private val deleteNoteAndTagDrafts: NoteAndTagUseCase.DeleteDrafts,
    private val notes: NoteUseCase.ObserveAll,
    private val ioDispatcher: CoroutineDispatcher,
    private val mediaMapper: MediaMapper,
    private val audioMapper: AudioMapper
) : CoroutineWorker(context, params) {

    @SuppressLint("RestrictedApi")
    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        return@withContext try {
            // 1. Database Cleanup
            cleanupNotes()
            cleanupConversations()
            deleteNoteAndTagDrafts()

            // 2. Fetch all current notes to identify valid files
            val allNotes = notes().first()

            // 3. Extract valid paths using your mappers
            val validAudioPaths = audioMapper.fromDomain(
                allNotes.map { it.data.audios }
            ).map { it.path } // Extracting the 'path' property from Audio model

            val videoPaths = mediaMapper.fromDomain(
                allNotes.map { it.data.medias }
            ).filter { it.isVideo }.map { it.uri }

            val imagePaths = mediaMapper.fromDomain(
                allNotes.map { it.data.medias }
            ).map { it.uri }

            // 4. Run File System Cleanup
            cleanupOrphanedAudios(context, validAudioPaths, ioDispatcher)
            cleanupOrphanedImages(context, imagePaths, ioDispatcher)
            cleanupOrphanedVideos(context, videoPaths, ioDispatcher)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}