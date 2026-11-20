package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Settings
import zouitel.neighborhood.domain.model.Settings as OutSettings

class SettingsMapper {
    fun toDomain(settings: Settings) = OutSettings(
        layout = settings.layout,
        ordination = settings.ordination,
        themeMode = settings.themeMode,
        fontFamily = settings.fontFamily,
        fontSize = settings.fontSize,
        cornerStyle = settings.cornerStyle,
        isSound = settings.isSound,
        isLocked = settings.isLocked,
        isScreenshot = settings.isScreenshot,
        isZenMode = settings.isZenMode,
        isShakable = settings.isShakable,
        liquidGlassEnabled = settings.liquidGlassEnabled,
        liquidGlassBlurRadius = settings.liquidGlassBlurRadius,
        liquidGlassRefraction = settings.liquidGlassRefraction,
        liquidGlassTint = settings.liquidGlassTint
    )

    fun fromDomain(settings: OutSettings) = Settings(
        layout = settings.layout,
        ordination = settings.ordination,
        themeMode = settings.themeMode,
        fontFamily = settings.fontFamily,
        fontSize = settings.fontSize,
        cornerStyle = settings.cornerStyle,
        isSound = settings.isSound,
        isLocked = settings.isLocked,
        isScreenshot = settings.isScreenshot,
        isZenMode = settings.isZenMode,
        isShakable = settings.isShakable,
        liquidGlassEnabled = settings.liquidGlassEnabled,
        liquidGlassBlurRadius = settings.liquidGlassBlurRadius,
        liquidGlassRefraction = settings.liquidGlassRefraction,
        liquidGlassTint = settings.liquidGlassTint
    )
}
