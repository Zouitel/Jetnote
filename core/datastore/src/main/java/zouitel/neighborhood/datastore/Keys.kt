package zouitel.neighborhood.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import zouitel.neighborhood.datastore.Constants.LOCK_MODE
import zouitel.neighborhood.datastore.Constants.MUTE
import zouitel.neighborhood.datastore.Constants.SCREENSHOT_BLOCK
import zouitel.neighborhood.datastore.Constants.SHAKE_TO_REPORT
import zouitel.neighborhood.datastore.Constants.THEME
import zouitel.neighborhood.datastore.Constants.ZEN_MODE

object Keys {
    val LAYOUT = stringPreferencesKey(Constants.LAYOUT)
    val ORDINATION = stringPreferencesKey(Constants.ORDINATION)
    val DARK_THEME = booleanPreferencesKey(THEME)
    val IS_MUTED = booleanPreferencesKey(MUTE)
    val IS_LOCK_MODE = booleanPreferencesKey(LOCK_MODE)
    val IS_SCREENSHOT_BLOCKED = booleanPreferencesKey(SCREENSHOT_BLOCK)
    val IS_ZEN_MODE = booleanPreferencesKey(ZEN_MODE)
    val IS_SHAKABLE = booleanPreferencesKey(SHAKE_TO_REPORT)
    val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
    val LIQUID_GLASS_BLUR_RADIUS = floatPreferencesKey("liquid_glass_blur_radius")
    val LIQUID_GLASS_REFRACTION = floatPreferencesKey("liquid_glass_refraction")
    val LIQUID_GLASS_TINT = floatPreferencesKey("liquid_glass_tint")

    @Deprecated("Use IS_MUTED; the stored preference represents the mute setting.")
    val IS_SOUND = IS_MUTED

    @Deprecated("Use IS_SCREENSHOT_BLOCKED; the stored preference represents screenshot blocking.")
    val IS_SCREENSHOT = IS_SCREENSHOT_BLOCKED
}