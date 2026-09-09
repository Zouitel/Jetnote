package zouitel.neighborhood.ai.provider

import androidx.compose.runtime.Stable
import zouitel.neighborhood.ai.provider.providers.ClaudeProvider
import zouitel.neighborhood.ai.provider.providers.GoogleProvider
import zouitel.neighborhood.ai.provider.providers.GlobalProvider
import okhttp3.OkHttpClient

@Stable
class ProviderManager(client: OkHttpClient) {
    private val providers = mutableMapOf<String, Provider<*>>()

    init {
        registerProvider("google", GoogleProvider(client))
        registerProvider("claude", ClaudeProvider(client))
        registerProvider("global", GlobalProvider(client))
    }

    fun registerProvider(name: String, provider: Provider<*>) {
        providers[name] = provider
    }

    fun getProvider(name: String): Provider<*> {
        return providers[name] ?: throw IllegalArgumentException("Provider not found: $name")
    }

    fun <T : ProviderSetting> getProviderByType(setting: T): Provider<T> {
        @Suppress("UNCHECKED_CAST")
        return when (setting) {
            is ProviderSetting.Google -> getProvider("google")
            is ProviderSetting.Claude -> getProvider("claude")
            is ProviderSetting.GlobalAI -> getProvider("global")
        } as Provider<T>
    }
}
