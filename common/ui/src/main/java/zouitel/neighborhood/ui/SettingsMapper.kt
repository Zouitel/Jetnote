package zouitel.neighborhood.ui

import zouitel.neighborhood.domain.model.Settings as OutSettings

class SettingsMapper {
    fun fromDomain(settings: OutSettings) = Settings(
        layout = settings.layout,
        ordination = settings.ordination,
        darkTheme = settings.darkTheme,
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

    fun toDomain(settings: Settings) = OutSettings(
        layout = settings.layout,
        ordination = settings.ordination,
        darkTheme = settings.darkTheme,
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