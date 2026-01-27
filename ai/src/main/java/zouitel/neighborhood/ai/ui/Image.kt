package zouitel.neighborhood.ai.ui

import kotlinx.serialization.Serializable

@Serializable
data class ImageGenerationResult(
    val items: List<ImageGenerationItem>
)

@Serializable
data class ImageGenerationItem(
    val data: String,
    val mimeType: String
)

@Serializable
enum class ImageAspectRatio {
    SQUARE,
    LANDSCAPE,
    PORTRAIT
}
