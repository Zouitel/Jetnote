package zouitel.neighborhood.security.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.lazyModule
import zouitel.neighborhood.security.NativeSecurityManager
import zouitel.neighborhood.security.detection.SecurityHandler
import zouitel.neighborhood.security.encrition.EncryptedPreferences

val rootedLazyModule = lazyModule {
    single { NativeSecurityManager() }
    single { SecurityHandler(androidContext()) }
    single {
        EncryptedPreferences(androidContext())
    }
}