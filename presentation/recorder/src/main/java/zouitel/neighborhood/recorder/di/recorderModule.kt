package zouitel.neighborhood.recorder.di

import androidx.media3.exoplayer.ExoPlayer
import zouitel.neighborhood.recorder.recorder.RecorderRepo
import zouitel.neighborhood.recorder.recorder.RecorderRepoImpl
import zouitel.neighborhood.recorder.ui.RecorderScreenModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val recorderModule = module {
    single {
        RecorderRepoImpl(androidContext())
    }

    singleOf(::RecorderRepoImpl) bind RecorderRepo::class

    factoryOf(::RecorderScreenModel)

    single {
        ExoPlayer.Builder(androidContext())
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}