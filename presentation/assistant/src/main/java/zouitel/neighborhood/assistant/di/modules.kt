package zouitel.neighborhood.assistant.di

import zouitel.neighborhood.ai.provider.ProviderManager
import zouitel.neighborhood.assistant.AppScope
import zouitel.neighborhood.assistant.ai.AIRequestInterceptor
import zouitel.neighborhood.assistant.ai.GenerationHandler
import zouitel.neighborhood.assistant.ai.transformers.AssistantTemplateLoader
import zouitel.neighborhood.assistant.ai.transformers.TemplateTransformer
import zouitel.neighborhood.assistant.mapper.AssistantMapper
import zouitel.neighborhood.assistant.mapper.ConversationMapper
import zouitel.neighborhood.assistant.mapper.ModelMapper
import zouitel.neighborhood.assistant.mapper.ProviderMapper
import zouitel.neighborhood.assistant.service.ChatService
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreenModel
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.assistant.ui.ai_model.ModelsScreenModel
import zouitel.neighborhood.assistant.ui.provider.ProviderScreenModel
import zouitel.neighborhood.assistant.utils.JsonInstant
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.remoteConfig
import io.pebbletemplates.pebble.PebbleEngine
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.util.Locale
import java.util.concurrent.TimeUnit

val assistantModule = module {
    factoryOf(::AssistantMapper)
    factoryOf(::ConversationMapper)
    factoryOf(::ModelMapper)
    factoryOf(::ProviderMapper)

    factoryOf(::AssistantScreenModel)
    factoryOf(::ModelsScreenModel)
    factoryOf(::ProviderScreenModel)
    factoryOf(::ChatScreenModel)

    factoryOf(::ProviderManager)

    single<Json> { JsonInstant }

    single {
        Firebase.crashlytics
    }

    single {
        Firebase.remoteConfig
    }

    single {
        Firebase.analytics
    }

    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor(AIRequestInterceptor(remoteConfig = get()))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build()
    }

    single {
        ProviderManager(get())
    }
    
    single {
        ChatService(
            androidApplication(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single {
        GenerationHandler(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single {
        AssistantTemplateLoader()
    }
    single {
        PebbleEngine.Builder()
            .loader(get<AssistantTemplateLoader>())
            .defaultLocale(Locale.getDefault())
            .autoEscaping(false)
            .build()
    }
    single {
        TemplateTransformer(get())
    }
    single {
        AppScope()
    }
}