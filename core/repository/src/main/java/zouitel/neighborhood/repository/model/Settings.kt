package zouitel.neighborhood.repository.model

data class Settings(
    val layout: String,
    val ordination: String,
    val darlTheme: Boolean,
    val isSound: Boolean,
    val isLocked: Boolean,
    val isScreenshot: Boolean,
    val isZenMode: Boolean,
    val isShakable: Boolean,
    val liquidGlassEnabled: Boolean = true,
    val liquidGlassBlurRadius: Float = 18f,
    val liquidGlassRefraction: Float = 12f,
    val liquidGlassTint: Float = 0.12f
)