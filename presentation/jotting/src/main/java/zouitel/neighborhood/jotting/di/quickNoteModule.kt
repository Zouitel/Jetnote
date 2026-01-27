package zouitel.neighborhood.jotting.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import zouitel.neighborhood.jotting.mapper.QuickDataMapper
import zouitel.neighborhood.jotting.ui.QuickDataScreenModel

val quickNoteModule = module {
    factoryOf(::QuickDataMapper)
    factoryOf(::QuickDataScreenModel)
}