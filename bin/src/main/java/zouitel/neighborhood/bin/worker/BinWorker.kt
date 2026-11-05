package zouitel.neighborhood.bin.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import zouitel.neighborhood.domain.usecase.ConversationUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase

/**
 * Permanently removes items already placed in the bin.
 *
 * File-system orphan collection is deliberately not performed here: the current
 * inventory only observes notes and does not include every possible reference
 * (notably conversation message attachments). Deleting against an incomplete
 * inventory can destroy files that are still in use.
 */
class BinWorker(
    context: Context,
    params: WorkerParameters,
    private val cleanupNotes: NoteUseCase.Cleanup,
    private val cleanupConversations: ConversationUseCase.Cleanup,
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        try {
            cleanupNotes()
            cleanupConversations()
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Bin cleanup failed; WorkManager may retry it.", exception)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "BinWorker"
    }
}
