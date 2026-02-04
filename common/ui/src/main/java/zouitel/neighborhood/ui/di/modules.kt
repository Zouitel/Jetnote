package zouitel.neighborhood.ui.di

import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.SettingsMapper
import org.koin.dsl.module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import zouitel.neighborhood.ui.hightlight.Highlighter

val systemDesignModule = module {
    factoryOf(::SettingsScreenModel)
    factoryOf(::SettingsMapper)
    singleOf(::Highlighter)
}