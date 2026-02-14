package zouitel.neighborhood.security.di

import zouitel.neighborhood.security.RootScreenModel
import zouitel.neighborhood.security.mapper.RootMapper
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.lazyModule
import zouitel.neighborhood.security.NativeSecurityManager

val rootedLazyModule = lazyModule {
    factory { RootScreenModel(get(), get()) }
    factoryOf(::RootMapper)
    single { NativeSecurityManager() }
}