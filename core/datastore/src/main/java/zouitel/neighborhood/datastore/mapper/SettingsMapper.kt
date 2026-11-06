package zouitel.neighborhood.datastore.mapper

import zouitel.neighborhood.datastore.model.Settings

class SettingsMapper {
    fun toRepo(settings: Settings) = zouitel.neighborhood.repository.model.Settings(
        layout = settings.layout,
        ordination = settings.ordination,
        darlTheme = settings.darkTheme,
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

    fun fromRepo(settings: zouitel.neighborhood.repository.model.Settings) = Settings(
        layout = settings.layout,
        ordination = settings.ordination,
        darkTheme = settings.darlTheme,
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