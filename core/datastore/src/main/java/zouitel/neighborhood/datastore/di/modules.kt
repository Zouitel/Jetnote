package zouitel.neighborhood.datastore.di

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import zouitel.neighborhood.datastore.Constants.DS_NAME
import zouitel.neighborhood.datastore.mapper.SettingsMapper
import zouitel.neighborhood.datastore.datasourceImpl.SettingsDataSourceImpl
import zouitel.neighborhood.repository.datasource.SettingsDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.lazyModule

val datastoreLazyModule = lazyModule {
    single {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            )
        ) {
            androidContext().preferencesDataStoreFile(DS_NAME)
        }
    }

    factoryOf(::SettingsMapper)
    singleOf(::SettingsDataSourceImpl) bind SettingsDataSource::class
}
