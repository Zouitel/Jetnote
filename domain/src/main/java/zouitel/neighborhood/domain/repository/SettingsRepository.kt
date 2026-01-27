package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val observe: Flow<Settings>
    suspend fun update(settings: Settings)
}