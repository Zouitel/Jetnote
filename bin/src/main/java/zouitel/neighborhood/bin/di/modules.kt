package zouitel.neighborhood.bin.di

import zouitel.neighborhood.bin.BinScreenModel
import zouitel.neighborhood.bin.worker.BinWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import zouitel.neighborhood.bin.mapper.AudioMapper
import zouitel.neighborhood.bin.mapper.MediaMapper

val binModule = module {
    factoryOf(::BinScreenModel)
    factoryOf(::MediaMapper)
    factoryOf(::AudioMapper)

    worker {
        BinWorker(
            androidContext(), get(), get(), get(),
            deleteNoteAndTagDrafts = get(),
            notes = get(),
            ioDispatcher = get(),
            mediaMapper = get(),
            audioMapper = get()
        )
    }
}