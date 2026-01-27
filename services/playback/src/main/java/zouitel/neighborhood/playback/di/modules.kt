package zouitel.neighborhood.playback.di

import androidx.media3.session.MediaSessionService
import zouitel.neighborhood.playback.PlaybackService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.lazyModule

val playbackLazyModule = lazyModule {
    singleOf(::PlaybackService) bind MediaSessionService::class
}