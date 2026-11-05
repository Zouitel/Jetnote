package zouitel.neighborhood.feedback.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

/**
 * Direct repository writes from the Android client are disabled.
 *
 * Restore submission only after a trusted backend authenticates requests,
 * validates feedback, rate-limits callers, and holds repository credentials
 * outside the distributed application.
 */
class FeedbackWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val output = Data.Builder()
            .putString(
                "ERROR_MESSAGE",
                "Feedback submission is temporarily unavailable until secure server-side submission is configured."
            )
            .build()
        return Result.failure(output)
    }
}
