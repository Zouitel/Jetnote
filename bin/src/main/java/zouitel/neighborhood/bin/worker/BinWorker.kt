package zouitel.neighborhood.bin.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import zouitel.neighborhood.domain.usecase.ConversationUseCase
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase

class BinWorker(
    context: Context,
    params: WorkerParameters,
    private val cleanupNotes: NoteUseCase.Cleanup,
    private val cleanupConversations: ConversationUseCase.Cleanup,
    private val deleteNoteAndTagDrafts: NoteAndTagUseCase.DeleteDrafts,
    private val purgeExpiredNotes: DataUseCase.PurgeExpired,
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        try {
            purgeExpiredNotes()
            cleanupNotes()
            cleanupConversations()
            deleteNoteAndTagDrafts()
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Trash/bin cleanup failed; WorkManager may retry it.", exception)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "BinWorker"
    }
}
