package zouitel.neighborhood.datastore.model

import zouitel.neighborhood.datastore.Constants

data class Settings(
    val layout: String = Constants.DEFAULT_LAYOUT,
    val ordination: String = Constants.DEFAULT_ORDER,
    val themeMode: String = Constants.DEFAULT_THEME_MODE,
    val fontFamily: String = Constants.DEFAULT_FONT_FAMILY,
    val fontSize: String = Constants.DEFAULT_FONT_SIZE,
    val isSound: Boolean = false,
    val isLocked: Boolean = false,
    val isScreenshot: Boolean = false,
    val isZenMode: Boolean = false,
    val isShakable: Boolean = true,
    val liquidGlassEnabled: Boolean = true,
    val liquidGlassBlurRadius: Float = 18f,
    val liquidGlassRefraction: Float = 12f,
    val liquidGlassTint: Float = 0.12f
)
