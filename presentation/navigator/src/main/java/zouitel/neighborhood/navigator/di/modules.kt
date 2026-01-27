package zouitel.neighborhood.navigator.di

import androidx.compose.ui.platform.ComposeView
import cafe.adriel.voyager.core.registry.screenModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.createdAtStart
import zouitel.neighborhood.account.accountModule
import zouitel.neighborhood.assistant.di.assistantModule
import zouitel.neighborhood.audio.di.audioPlayerModule
import zouitel.neighborhood.workspace.provider.SharedScreen
import zouitel.neighborhood.link.di.linksModule
import zouitel.neighborhood.workspace.di.workplaceModule
import zouitel.neighborhood.jotting.di.quickNoteModule
import zouitel.neighborhood.recorder.di.recorderModule
import zouitel.neighborhood.navigator.archive_screen.ArchiveScreen
import zouitel.neighborhood.navigator.archive_screen.ArchiveScreenModel
import zouitel.neighborhood.navigator.main_screen.MainScreen
import zouitel.neighborhood.navigator.main_screen.MainScreenModel
import zouitel.neighborhood.navigator.mapper.NoteMapper
import zouitel.neighborhood.search.di.searchModule
import zouitel.neighborhood.tag.di.tagsModule
import zouitel.neighborhood.widget.di.widgetModule
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.lazyModule
import org.koin.dsl.module

val screensModule = module {
    factoryOf(::MainScreenModel)
    factoryOf(::NoteMapper)
    factoryOf(::ArchiveScreen)
    factoryOf(::ArchiveScreenModel)

    includes(
        audioPlayerModule,
        linksModule,
        workplaceModule,
        quickNoteModule,
        recorderModule,
        tagsModule,
        widgetModule,
        searchModule,
        accountModule,
        assistantModule
    )
}

val initializerLazyModule = lazyModule {
    single {
        ComposeView(androidContext())
    } withOptions {
        named("compose-view")
        createdAtStart()
    }
}
val screensScreenModule = screenModule {
    register<SharedScreen.MainScreen> {
        MainScreen()
    }
}

