package zouitel.neighborhood.ai.provider

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Model(
    val uid: String? = "",
    val modelId: String = "",
    val providerId: Long,
    val displayName: String = "",
    val logoUrl: String? = null,
    val type: ModelType = ModelType.CHAT,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val inputModalities: List<Modality> = listOf(Modality.TEXT),
    val outputModalities: List<Modality> = listOf(Modality.TEXT),
    val abilities: List<ModelAbility> = emptyList(),
    val tools: Set<BuiltInTools> = emptySet(),
    val providerOverwrite: ProviderSetting? = null
)

@Serializable
enum class ModelType {
    CHAT,
    IMAGE,
    EMBEDDING;
}

@Serializable
enum class Modality {
    TEXT,
    IMAGE;
}

@Serializable
enum class ModelAbility {
    TOOL,
    REASONING;
}

// Built-in tool options provided by the model (provider).
@Serializable
sealed class BuiltInTools {
    // https://ai.google.dev/gemini-api/docs/google-search?hl=zh-cn
    @Serializable
    @SerialName("search")
    data object Search : BuiltInTools()

    // https://ai.google.dev/gemini-api/docs/url-context?hl=zh-cn
    @Serializable
    @SerialName("url_context")
    data object UrlContext : BuiltInTools()
}



