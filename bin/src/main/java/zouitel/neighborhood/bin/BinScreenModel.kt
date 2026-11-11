package zouitel.neighborhood.bin

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.bin.worker.BinWorker
import java.util.concurrent.TimeUnit

class BinScreenModel(application: Application): ScreenModel {
    private val workManager = WorkManager.getInstance(application)

    fun initBinWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresStorageNotLow(true)
            .build()

        // Run a cleanup when the app next goes to the background.
        val immediate = OneTimeWorkRequestBuilder<BinWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.KEEP, immediate)

        // Keep a daily sweep scheduled so expired notes are purged while the app is closed.
        val periodic = PeriodicWorkRequestBuilder<BinWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, KEEP, periodic)
    }

    companion object {
        const val WORK_NAME = "unique_bin_work"
        const val IMMEDIATE_WORK_NAME = "unique_bin_work_immediate"
    }
}
