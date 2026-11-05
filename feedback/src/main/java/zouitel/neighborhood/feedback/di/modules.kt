package zouitel.neighborhood.feedback.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module
import zouitel.neighborhood.feedback.ui.FeedbackScreenModel
import zouitel.neighborhood.feedback.worker.FeedbackUploadWorker
import zouitel.neighborhood.feedback.worker.FeedbackWorker

val feedbackModule = module {
    factory { FeedbackScreenModel(get()) }

    worker {
        FeedbackWorker(
            context = androidContext(),
            params = get()
        )
    }

    workerOf(::FeedbackUploadWorker)
}
