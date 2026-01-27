package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsDataSource {
    val observe: Flow<Settings>
    suspend fun update(settings: Settings)
}