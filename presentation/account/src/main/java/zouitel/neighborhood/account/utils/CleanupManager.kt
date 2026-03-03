package zouitel.neighborhood.account.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import zouitel.neighborhood.account.worker.DriveCleanupWorker
import java.util.concurrent.TimeUnit

class DriveCleanupScreenModel(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    // Call this when the user manually clicks "Empty Trash"
    fun executeManualCleanup() {
        // 1. Constraints for the Drive worker (Needs Internet!)
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 2. Setup the Local Worker
        val localCleanupWork = OneTimeWorkRequestBuilder<DriveCleanupWorker>()
            .build()

        // 3. Setup the Cloud Worker
        val cloudCleanupWork = OneTimeWorkRequestBuilder<DriveCleanupWorker>()
            .setConstraints(networkConstraints)
            .build()

        // 4. CHAIN THEM TOGETHER: Local first, then Cloud
        workManager.beginUniqueWork(
            "manual_cleanup_chain",
            ExistingWorkPolicy.REPLACE,
            cloudCleanupWork
        ).enqueue()
    }

    // Call this ONCE when your app starts (e.g., in your Application class or MainScreen)
    fun schedulePeriodicCleanup() {
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Don't drain battery in background
            .build()

        // Runs once every 7 days
        val periodicCloudCleanup = PeriodicWorkRequestBuilder<DriveCleanupWorker>(7, TimeUnit.DAYS)
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "weekly_cloud_cleanup",
            ExistingPeriodicWorkPolicy.KEEP, // If already scheduled, leave it alone
            periodicCloudCleanup
        )
    }
}