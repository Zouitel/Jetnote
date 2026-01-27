package zouitel.neighborhood.ui.di

import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.SettingsMapper
import org.koin.dsl.module
import org.koin.core.module.dsl.factoryOf

val systemDesignModule = module {
    factoryOf(::SettingsScreenModel)
    factoryOf(::SettingsMapper)
}