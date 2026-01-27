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
        isShakable = settings.isShakable
    )

    fun toDomain(settings: Settings) = OutSettings(
        layout = settings.layout,
        ordination = settings.ordination,
        darkTheme = settings.darkTheme,
        isSound = settings.isSound,
        isLocked = settings.isLocked,
        isScreenshot = settings.isScreenshot,
        isZenMode = settings.isZenMode,
        isShakable = settings.isShakable
    )
}