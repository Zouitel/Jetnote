package zouitel.neighborhood.datastore.datasourceImpl

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import zouitel.neighborhood.datastore.Constants
import zouitel.neighborhood.datastore.Keys
import zouitel.neighborhood.datastore.mapper.SettingsMapper
import zouitel.neighborhood.repository.datasource.SettingsDataSource
import zouitel.neighborhood.repository.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class SettingsDataSourceImpl(
    private val datastore: DataStore<Preferences>,
    private val mapper: SettingsMapper
) : SettingsDataSource {

    override val observe: Flow<Settings>
        get() = datastore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val themeMode = preferences[Keys.THEME_MODE]
                    ?: preferences[Keys.DARK_THEME]?.let { if (it) "dark" else "light" }
                    ?: Constants.DEFAULT_THEME_MODE

                mapper.toRepo(
                    zouitel.neighborhood.datastore.model.Settings(
                        layout = preferences[Keys.LAYOUT] ?: Constants.DEFAULT_LAYOUT,
                        ordination = preferences[Keys.ORDINATION] ?: Constants.DEFAULT_ORDER,
                        themeMode = themeMode,
                        isSound = preferences[Keys.IS_MUTED] ?: false,
                        isLocked = preferences[Keys.IS_LOCK_MODE] ?: false,
                        isScreenshot = preferences[Keys.IS_SCREENSHOT_BLOCKED] ?: false,
                        isZenMode = preferences[Keys.IS_ZEN_MODE] ?: false,
                        isShakable = preferences[Keys.IS_SHAKABLE] ?: true,
                        liquidGlassEnabled = preferences[Keys.LIQUID_GLASS_ENABLED] ?: true,
                        liquidGlassBlurRadius = preferences[Keys.LIQUID_GLASS_BLUR_RADIUS] ?: 18f,
                        liquidGlassRefraction = preferences[Keys.LIQUID_GLASS_REFRACTION] ?: 12f,
                        liquidGlassTint = preferences[Keys.LIQUID_GLASS_TINT] ?: 0.12f
                    )
                )
            }

    override suspend fun update(settings: Settings) {
        val dataStoreSettings = mapper.fromRepo(settings)

        datastore.edit { preferences ->
            preferences[Keys.LAYOUT] = dataStoreSettings.layout
            preferences[Keys.THEME_MODE] = dataStoreSettings.themeMode
            preferences.remove(Keys.DARK_THEME)
            preferences[Keys.ORDINATION] = dataStoreSettings.ordination
            preferences[Keys.IS_MUTED] = dataStoreSettings.isSound
            preferences[Keys.IS_LOCK_MODE] = dataStoreSettings.isLocked
            preferences[Keys.IS_SCREENSHOT_BLOCKED] = dataStoreSettings.isScreenshot
            preferences[Keys.IS_ZEN_MODE] = dataStoreSettings.isZenMode
            preferences[Keys.IS_SHAKABLE] = dataStoreSettings.isShakable
            preferences[Keys.LIQUID_GLASS_ENABLED] = dataStoreSettings.liquidGlassEnabled
            preferences[Keys.LIQUID_GLASS_BLUR_RADIUS] = dataStoreSettings.liquidGlassBlurRadius
            preferences[Keys.LIQUID_GLASS_REFRACTION] = dataStoreSettings.liquidGlassRefraction
            preferences[Keys.LIQUID_GLASS_TINT] = dataStoreSettings.liquidGlassTint
        }
    }
)