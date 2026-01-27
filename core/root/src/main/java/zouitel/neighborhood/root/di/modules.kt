package zouitel.neighborhood.root.di

import zouitel.neighborhood.root.RootManager
import zouitel.neighborhood.root.datasourceImpl.RootDataSourceImpl
import zouitel.neighborhood.root.mapper.RootMapper
import zouitel.neighborhood.repository.datasource.RootDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.lazyModule

val systemLazyModule = lazyModule {
    factoryOf(::RootMapper)
    single { RootManager(androidContext()) }
    singleOf(::RootDataSourceImpl) bind RootDataSource::class
}