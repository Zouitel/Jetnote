package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Root as OutRoot
import zouitel.neighborhood.domain.repository.RootRepository
import zouitel.neighborhood.repository.datasource.RootDataSource
import zouitel.neighborhood.repository.mapper.RootMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RootRepositoryImpl(
    private val dataSource: RootDataSource,
    private val mapper: RootMapper
): RootRepository {
    override val isDeviceRooted: Flow<Result<OutRoot>>
        get() = dataSource.isDeviceRooted.map { result ->
            result.mapCatching { mapper.toDomain(it) }
        }
}