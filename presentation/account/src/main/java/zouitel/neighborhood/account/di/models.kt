package zouitel.neighborhood.account.di

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import zouitel.neighborhood.account.ui.AccountScreenModel
import zouitel.neighborhood.account.worker.AudiosDownloadsWorker
import zouitel.neighborhood.account.worker.AudiosUploadWorker
import zouitel.neighborhood.account.worker.DataUploadWorker
import zouitel.neighborhood.account.utils.DriveCleanupScreenModel
import zouitel.neighborhood.account.worker.DriveCleanupWorker
import zouitel.neighborhood.account.worker.MoviesDownloadsWorker
import zouitel.neighborhood.account.worker.MoviesUploadWorker
import zouitel.neighborhood.account.worker.OthersDownloadsWorker
import zouitel.neighborhood.account.worker.OthersUploadWorker
import zouitel.neighborhood.account.worker.PicturesDownloadsWorker
import zouitel.neighborhood.account.worker.PicturesUploadWorker
import zouitel.neighborhood.account.worker.SyncDataWorker
import zouitel.neighborhood.account.ui.SyncScreenModel
import zouitel.neighborhood.account.mapper.AudioMapper
import zouitel.neighborhood.account.mapper.MediaMapper

val accountModule = module {
    factoryOf(::SyncScreenModel)
    factoryOf(::AccountScreenModel)
    factoryOf(::DriveCleanupScreenModel)
    factoryOf(::MediaMapper)
    factoryOf(::AudioMapper)

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
    workerOf(::DriveCleanupWorker)
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