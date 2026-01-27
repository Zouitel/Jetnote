package zouitel.neighborhood.bin.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Suppress("UNREACHABLE_CODE")
class BinWorker(
    private val context: Context,
    params: WorkerParameters,
    private val deleteNoteAndTagDrafts: NoteAndTagUseCase.DeleteDrafts,
    private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(context, params) {
    @SuppressLint("RestrictedApi")
    override suspend fun doWork(): Result = withContext(ioDispatcher){
        return@withContext try {
//            getMediaDrafts().forEach {
//                context.contentResolver.delete(it.toUri(), null, null)
//            }
            deleteNoteAndTagDrafts()

            return@withContext Result.Success()
        } catch (e: Exception) {
            return@withContext Result.failure()
        }
    }
}