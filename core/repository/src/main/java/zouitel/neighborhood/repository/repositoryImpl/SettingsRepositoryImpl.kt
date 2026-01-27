package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Settings as OutSettings
import zouitel.neighborhood.domain.repository.SettingsRepository
import zouitel.neighborhood.repository.datasource.SettingsDataSource
import zouitel.neighborhood.repository.mapper.SettingsMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataSource: SettingsDataSource,
    private val mapper: SettingsMapper
): SettingsRepository {
    override val observe: Flow<OutSettings>
        get() = dataSource.observe.map { mapper.toDomain(it) }

    override suspend fun update(settings: OutSettings) {
        dataSource.update(mapper.fromDomain(settings))
    }
}