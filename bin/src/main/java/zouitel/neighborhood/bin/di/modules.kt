package zouitel.neighborhood.bin.di

import zouitel.neighborhood.bin.BinScreenModel
import zouitel.neighborhood.bin.worker.BinWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val binModule = module {
    factoryOf(::BinScreenModel)

    worker {
        BinWorker(
            androidContext(),
            get(),
            get(),
            get(),
            deleteNoteAndTagDrafts = get(),
            ioDispatcher = get()
        )
    }
}
