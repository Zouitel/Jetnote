package zouitel.neighborhood.account.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import zouitel.neighborhood.account.worker.DriveCleanupWorker

class DriveCleanupScreenModel(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Keep the manual action observable in WorkManager. The worker currently fails
     * closed with an explanatory error because the app cannot yet prove ownership
     * of remote files using stable Drive IDs.
     */
    fun executeManualCleanup() {
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val cleanupWork = OneTimeWorkRequestBuilder<DriveCleanupWorker>()
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniqueWork(
            "manual_cleanup_chain",
            ExistingWorkPolicy.REPLACE,
            cleanupWork
        )
    }

    /**
     * Automatic cleanup must remain disabled until Drive ownership metadata and
     * stable file-ID reconciliation are implemented and tested.
     */
    fun schedulePeriodicCleanup() {
        workManager.cancelUniqueWork("weekly_cloud_cleanup")
    }
}
