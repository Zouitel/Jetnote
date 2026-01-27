package zouitel.neighborhood.ai.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class HttpProxy(
    val enabled: Boolean = false,
    val address: String? = null,
    val port: Int = 8080,
    val username: String? = null,
    val password: String? = null,
)
@Serializable
data class BalanceOption(
    val enabled: Boolean = false,
    val apiPath: String = "/credits",
    val resultPath: String = "data.total_usage"
)

@Serializable
sealed class ProviderSetting {
    abstract val id: Long
    abstract val enabled: Boolean
    abstract val name: String
    abstract val logoUrl: String?
    abstract val apiKey: String
    abstract val apiLink: String
    abstract val models: List<Model>
    abstract val proxy: HttpProxy
    abstract val balanceOption: BalanceOption

    abstract val builtIn: Boolean
    abstract val description: String

    abstract fun addModel(model: Model): ProviderSetting
    abstract fun editModel(model: Model): ProviderSetting
    abstract fun delModel(model: Model): ProviderSetting
    abstract fun moveMove(from: Int, to: Int): ProviderSetting
    abstract fun copyProvider(
        id: Long = this.id,
        enabled: Boolean = this.enabled,
        name: String = this.name,
        models: List<Model> = this.models,
        proxy: HttpProxy = this.proxy,
        balanceOption: BalanceOption = this.balanceOption,
        builtIn: Boolean = this.builtIn,
        description: String = this.description,
    ): ProviderSetting

    @Serializable
    @SerialName("openai")
    data class GlobalAI(
        override var id: Long = -1,
        override var enabled: Boolean = true,
        override var name: String = "GlobalAI",
        override val apiKey: String = "",
        override val apiLink: String = "https://www.google.com",
        override val logoUrl: String? = null,
        override var models: List<Model> = emptyList(),
        override val proxy: HttpProxy = HttpProxy(),
        override val balanceOption: BalanceOption = BalanceOption(),
        @Transient override val builtIn: Boolean = false,
        @Transient override val description: String = "",
        var baseUrl: String = "https://api.openai.com/v1",
        var chatCompletionsPath: String? = "/chat/completions",
        var useResponseApi: Boolean = false,
    ) : ProviderSetting() {
        override fun addModel(model: Model): ProviderSetting {
            return copy(models = models + model)
        }

        override fun editModel(model: Model): ProviderSetting {
            return copy(models = models.map { if (it.uid == model.uid) model.copy() else it })
        }

        override fun delModel(model: Model): ProviderSetting {
            return copy(models = models.filter { it.uid != model.uid })
        }

        override fun moveMove(
            from: Int,
            to: Int
        ): ProviderSetting {
            return copy(models = models.toMutableList().apply {
                val model = removeAt(from)
                add(to, model)
            })
        }

        override fun copyProvider(
            id: Long,
            enabled: Boolean,
            name: String,
            models: List<Model>,
            proxy: HttpProxy,
            balanceOption: BalanceOption,
            builtIn: Boolean,
            description: String
        ): ProviderSetting {
            return this.copy(
                id = id,
                enabled = enabled,
                name = name,
                models = models,
                builtIn = builtIn,
                description = description,
                proxy = proxy,
                balanceOption = balanceOption
            )
        }
    }

    @Serializable
    @SerialName("google")
    data class Google(
        override var id: Long = 1,
        override var enabled: Boolean = true,
        override val name: String = "Google",
        override val apiKey: String = "",
        override val apiLink: String = "https://aistudio.google.com/app/api-keys",
        override val logoUrl: String? = null,
        override var models: List<Model> = emptyList(),
        override val proxy: HttpProxy = HttpProxy(),
        override val balanceOption: BalanceOption = BalanceOption(),
        @Transient override val builtIn: Boolean = false,
        override val description: String = "",
        var baseUrl: String = "https://generativelanguage.googleapis.com/v1beta", // only for google AI
        var vertexAI: Boolean = false,
        var privateKey: String = "", // only for vertex AI
        var serviceAccountEmail: String = "", // only for vertex AI
        var location: String = "us-central1", // only for vertex AI
        var projectId: String = "", // only for vertex AI
    ) : ProviderSetting() {
        override fun addModel(model: Model): ProviderSetting {
            return copy(models = models + model)
        }

        override fun editModel(model: Model): ProviderSetting {
            return copy(models = models.map { if (it.uid == model.uid) model.copy() else it })
        }

        override fun delModel(model: Model): ProviderSetting {
            return copy(models = models.filter { it.uid != model.uid })
        }

        override fun moveMove(
            from: Int,
            to: Int
        ): ProviderSetting {
            return copy(models = models.toMutableList().apply {
                val model = removeAt(from)
                add(to, model)
            })
        }

        override fun copyProvider(
            id: Long,
            enabled: Boolean,
            name: String,
            models: List<Model>,
            proxy: HttpProxy,
            balanceOption: BalanceOption,
            builtIn: Boolean,
            description: String
        ): ProviderSetting {
            return this.copy(
                id = id,
                enabled = enabled,
                name = name,
                models = models,
                builtIn = builtIn,
                description = description,
                proxy = proxy,
                balanceOption = balanceOption
            )
        }
    }

    @Serializable
    @SerialName("claude")
    data class Claude(
        override var id: Long = 2,
        override var enabled: Boolean = true,
        override var name: String = "Claude",
        override val apiKey: String = "",
        override val apiLink: String = "https://console.anthropic.com/settings/keys",
        override val logoUrl: String? = null,
        override var models: List<Model> = emptyList(),
        override var proxy: HttpProxy = HttpProxy(),
        override val balanceOption: BalanceOption = BalanceOption(),
        @Transient override val builtIn: Boolean = false,
        @Transient override val description: String = "",
        var baseUrl: String = "https://api.anthropic.com/v1",
    ) : ProviderSetting() {
        override fun addModel(model: Model): ProviderSetting {
            return copy(models = models + model)
        }

        override fun editModel(model: Model): ProviderSetting {
            return copy(models = models.map { if (it.uid == model.uid) model.copy() else it })
        }

        override fun delModel(model: Model): ProviderSetting {
            return copy(models = models.filter { it.uid != model.uid })
        }

        override fun moveMove(
            from: Int,
            to: Int
        ): ProviderSetting {
            return copy(models = models.toMutableList().apply {
                val model = removeAt(from)
                add(to, model)
            })
        }

        override fun copyProvider(
            id: Long,
            enabled: Boolean,
            name: String,
            models: List<Model>,
            proxy: HttpProxy,
            balanceOption: BalanceOption,
            builtIn: Boolean,
            description: String
        ): ProviderSetting {
            return this.copy(
                id = id,
                enabled = enabled,
                name = name,
                models = models,
                proxy = proxy,
                balanceOption = balanceOption,
                builtIn = builtIn,
                description = description,
            )
        }
    }

    companion object {
        val Types by lazy {
            listOf(
                GlobalAI::class,
                Google::class,
                Claude::class,
            )
        }
    }
}
