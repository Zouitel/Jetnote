package zouitel.neighborhood.account.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * Drive orphan deletion is intentionally disabled until uploaded media has stable
 * Drive-file-ID ownership metadata. Local filenames/URIs are not sufficient proof
 * that a remote Drive file belongs to this installation or is safe to delete.
 */
class DriveCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reason = "Drive cleanup is disabled: stable Jetnote ownership metadata and Drive file IDs are required before deletion."
        Log.w(TAG, reason)

        // Fail visibly instead of reporting a successful cleanup that did nothing,
        // and, most importantly, never delete files based on filename comparison.
        return Result.failure(workDataOf(KEY_ERROR to reason))
    }

    private companion object {
        const val TAG = "DriveCleanupWorker"
        const val KEY_ERROR = "error"
    }
}
