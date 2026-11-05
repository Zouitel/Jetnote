package zouitel.neighborhood.audio.di

import zouitel.neighborhood.audio.audio.AudioRepository
import zouitel.neighborhood.audio.audio.LocalMediaDataSource
import zouitel.neighborhood.audio.player.PlaybackManager
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.audio.ui.list.AudioListScreenModel
import zouitel.neighborhood.audio.worker.CopyAudioWorker
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val audioPlayerModule = module {
    single { Dispatchers.IO }
    single { AudioRepository(get(), get()) }
    single { LocalMediaDataSource(androidContext(), get()) }
    single { PlaybackManager(androidContext()) }

    factory { AudioListScreenModel(androidApplication(), androidContext(), get()) }
    factory { AudioScreenModel(get(), get()) }

    worker { CopyAudioWorker(androidContext(), get(), get()) }
}
