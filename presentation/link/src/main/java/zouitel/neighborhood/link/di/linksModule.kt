package zouitel.neighborhood.link.di

import zouitel.neighborhood.link.mapper.LinkMapper
import zouitel.neighborhood.link.ui.LinkScreen
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.link.worker.LinkWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val linksModule = module {
    factoryOf(::LinkMapper)
    factoryOf(::LinkScreen)
    factory {
        LinkScreenModel(get(), get(), get(), get(), get(), get())
    }
    worker {
        LinkWorker(androidContext(), get(), get(), get(), get())
    }
}