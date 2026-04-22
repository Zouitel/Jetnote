package zouitel.neighborhood.jetnote

import android.app.Application
import android.os.Build
import cafe.adriel.voyager.core.registry.ScreenRegistry
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.androix.startup.KoinStartup
import org.koin.core.lazyModules
import org.koin.core.logger.Level
import org.koin.dsl.koinConfiguration
import zouitel.neighborhood.bin.di.binModule
import zouitel.neighborhood.database.di.databaseLazyModule
import zouitel.neighborhood.datastore.di.datastoreLazyModule
import zouitel.neighborhood.feedback.di.feedbackModule
import zouitel.neighborhood.navigator.di.initializerLazyModule
import zouitel.neighborhood.navigator.di.screensModule
import zouitel.neighborhood.navigator.di.screensScreenModule
import zouitel.neighborhood.permissions.permissionsModule
import zouitel.neighborhood.playback.di.playbackLazyModule
import zouitel.neighborhood.repository.di.localRepoLazyModule
import zouitel.neighborhood.security.di.rootedLazyModule
import zouitel.neighborhood.ui.di.systemDesignModule
import zouitel.neighborhood.workspace.di.feedbackScreenModule
import zouitel.neighborhood.workspace.di.tagsScreenModule
import zouitel.neighborhood.workspace.di.workplaceScreenModule

class NoteApplication: Application(), ImageLoaderFactory, KoinStartup {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this@NoteApplication)

        // Init remote config
        get<FirebaseRemoteConfig>().apply {
            setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds = 1800
            })
            fetchAndActivate()
        }

        System.loadLibrary("sqlcipher")
    }

    override fun onKoinStartup() = koinConfiguration {
        androidLogger(Level.INFO)
        androidContext(this@NoteApplication)
        workManagerFactory()
        lazyModules(
            initializerLazyModule,
            databaseLazyModule,
            localRepoLazyModule,
            datastoreLazyModule,
            rootedLazyModule,
            playbackLazyModule
        )
        modules(
            systemDesignModule,
            screensModule,
            permissionsModule,
            binModule,
            feedbackModule
        )

        ScreenRegistry {
            screensScreenModule()
            workplaceScreenModule()
            tagsScreenModule()
            feedbackScreenModule()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader(this)
            .newBuilder()
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(.5)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}