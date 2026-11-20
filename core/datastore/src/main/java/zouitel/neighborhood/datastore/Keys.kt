package zouitel.neighborhood.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import zouitel.neighborhood.datastore.Constants.FONT_FAMILY as FONT_FAMILY_KEY
import zouitel.neighborhood.datastore.Constants.LOCK_MODE
import zouitel.neighborhood.datastore.Constants.MUTE
import zouitel.neighborhood.datastore.Constants.SCREENSHOT_BLOCK
import zouitel.neighborhood.datastore.Constants.SHAKE_TO_REPORT
import zouitel.neighborhood.datastore.Constants.THEME
import zouitel.neighborhood.datastore.Constants.THEME_MODE as THEME_MODE_KEY
import zouitel.neighborhood.datastore.Constants.ZEN_MODE

object Keys {
    val LAYOUT = stringPreferencesKey(Constants.LAYOUT)
    val ORDINATION = stringPreferencesKey(Constants.ORDINATION)
    // Kept to migrate existing boolean preferences to the new three-state setting.
    val DARK_THEME = booleanPreferencesKey(THEME)
    val THEME_MODE: Preferences.Key<String> = stringPreferencesKey(THEME_MODE_KEY)
    val FONT_FAMILY: Preferences.Key<String> = stringPreferencesKey(FONT_FAMILY_KEY)
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
