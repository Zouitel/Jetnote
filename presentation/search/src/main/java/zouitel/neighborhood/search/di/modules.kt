package zouitel.neighborhood.search.di

import zouitel.neighborhood.search.mapper.NoteMapper
import zouitel.neighborhood.search.mapper.TagMapper
import zouitel.neighborhood.search.ui.SearchScreenModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val searchModule = module {
    factory {
        SearchScreenModel(
            get(),
            get(),
            get(),
            get()
        )
    }

    factoryOf(::TagMapper)
    factory {
        NoteMapper(get(), get())
    }
}

