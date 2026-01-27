package zouitel.neighborhood.ai.provider

import zouitel.neighborhood.ai.provider.providers.ClaudeProvider
import zouitel.neighborhood.ai.provider.providers.GoogleProvider
import zouitel.neighborhood.ai.provider.providers.GlobalProvider
import okhttp3.OkHttpClient

/**
 * Provider管理器，负责注册和获取Provider实例
 */
class ProviderManager(client: OkHttpClient) {
    private val providers = mutableMapOf<String, Provider<*>>()

    init {
        registerProvider("google", GoogleProvider(client))
        registerProvider("claude", ClaudeProvider(client))
        registerProvider("global", GlobalProvider(client))
    }

    /**
     * 注册Provider实例
     *
     * @param name Provider名称
     * @param provider Provider实例
     */
    fun registerProvider(name: String, provider: Provider<*>) {
        providers[name] = provider
    }

    /**
     * 获取Provider实例
     *
     * @param name Provider名称
     * @return Provider实例，如果不存在则返回null
     */
    fun getProvider(name: String): Provider<*> {
        return providers[name] ?: throw IllegalArgumentException("Provider not found: $name")
    }

    /**
     * 根据ProviderSetting获取对应的Provider实例
     *
     * @param setting Provider设置
     * @return Provider实例，如果不存在则返回null
     */
    fun <T : ProviderSetting> getProviderByType(setting: T): Provider<T> {
        @Suppress("UNCHECKED_CAST")
        return when (setting) {
            is ProviderSetting.Google -> getProvider("google")
            is ProviderSetting.Claude -> getProvider("claude")
            is ProviderSetting.GlobalAI -> getProvider("global")
        } as Provider<T>
    }
}
