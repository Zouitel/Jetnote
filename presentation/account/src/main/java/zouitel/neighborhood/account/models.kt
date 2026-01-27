package zouitel.neighborhood.account

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val accountModule = module {
    factoryOf(::SyncScreenModel)
    factoryOf(::AccountScreenModel)

    workerOf(::DataUploadWorker)
    workerOf(::PicturesUploadWorker)
    workerOf(::MoviesUploadWorker)
    workerOf(::OthersUploadWorker)
    workerOf(::AudiosUploadWorker)
    workerOf(::SyncDataWorker)
    workerOf(::PicturesDownloadsWorker)
    workerOf(::MoviesDownloadsWorker)
    workerOf(::OthersDownloadsWorker)
    workerOf(::AudiosDownloadsWorker)
    worker {
        SyncDataWorker(
            context = androidContext(),
            params = get(),
            ioDispatcher = get(),
            insertData = get(),
            insertNoteAndTags = get(),
            insertTags = get(),
            dataMapper = get(),
            noteAndTagMapper = get(),
            tagMapper = get()
        )
    }

    single {
        Firebase.auth
    }
}