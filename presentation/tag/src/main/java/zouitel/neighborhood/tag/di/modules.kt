package zouitel.neighborhood.tag.di

import zouitel.neighborhood.tag.mapper.NoteAndTagMapper
import zouitel.neighborhood.tag.mapper.TagMapper
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val tagsModule = module {
    factoryOf(::TagMapper)
    factoryOf(::NoteAndTagMapper)

    factoryOf(::NoteAndTagScreenModel)
    factoryOf(::TagScreenModel)
}

