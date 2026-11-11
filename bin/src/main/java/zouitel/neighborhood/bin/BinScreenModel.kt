package zouitel.neighborhood.bin

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.NetworkType
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

        val request = PeriodicWorkRequestBuilder<BinWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(WORK_NAME, KEEP, request)
    }

    companion object {
        const val WORK_NAME = "unique_bin_work"
    }
}
