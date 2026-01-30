package zouitel.neighborhood.workspace.di

import cafe.adriel.voyager.core.registry.screenModule
import zouitel.neighborhood.tag.ui.TagsScreen
import zouitel.neighborhood.workspace.mapper.DataMapper
import zouitel.neighborhood.workspace.notifications.AlarmManagerScreenModel
import zouitel.neighborhood.workspace.notifications.NotificationReceiver
import zouitel.neighborhood.workspace.provider.SharedScreen
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreen
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreenModel
import zouitel.neighborhood.workspace.worker.CopyMediaWorker
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import zouitel.neighborhood.feedback.FeedbackScreen
import zouitel.neighborhood.feedback.FeedbackSheetScreen

val workplaceModule = module {
    factoryOf(::DataMapper)
    factoryOf(::AlarmManagerScreenModel)
    factory {
        WorkplaceScreenModel(androidApplication())
    }
    factory {
        DataScreenModel(get(), get(), get(), get(), get(), get())
    }
    single {
        NotificationReceiver()
    }
    worker {
        CopyMediaWorker(androidContext(), get(), get())
    }
}

val workplaceScreenModule = screenModule {
    register<SharedScreen.Workplace> {
        WorkplaceScreen(
            data = it.data,
            isNew = it.isNew,
            )
    }
}
val feedbackScreenModule = screenModule {
    register<SharedScreen.FeedbackSheetScreen> {
        FeedbackSheetScreen()
    }
    register<SharedScreen.FeedbackScreen>{
        FeedbackScreen()
    }
}
val tagsScreenModule = screenModule {
    register<SharedScreen.Tags> {
        TagsScreen(it.uid, it.background)
    }
}
