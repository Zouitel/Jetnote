package zouitel.neighborhood.widget.di

import zouitel.neighborhood.widget.mapper.WidgetMapper
import zouitel.neighborhood.widget.ui.QuickWidget
import zouitel.neighborhood.widget.ui.WidgetScreen
import zouitel.neighborhood.widget.ui.WidgetScreenModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val widgetModule = module {
    factory {
        WidgetMapper(get(), get())
    }

    factory {
        WidgetScreenModel(get(), get())
    }
    factoryOf(::WidgetScreen)
    factoryOf(::QuickWidget)
}