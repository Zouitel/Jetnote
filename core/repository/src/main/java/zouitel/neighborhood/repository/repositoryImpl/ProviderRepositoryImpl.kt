package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Provider
import zouitel.neighborhood.domain.repository.ProviderRepo
import zouitel.neighborhood.repository.datasource.ProviderDatasource
import zouitel.neighborhood.repository.mapper.ProviderMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProviderRepositoryImpl(
    private val datasource: ProviderDatasource,
    private val mapper: ProviderMapper
): ProviderRepo {
    override val observeAll: Flow<List<Provider>>
        get() = datasource.observeAll.map { mapper.toDomain(it) }

    override suspend fun observeById(id: Long): Flow<Provider> {
        return datasource.observeById(id).map { mapper.toDomain(it) }
    }

    override suspend fun upsert(provider: Provider) {
        datasource.upsert(mapper.fromDomain(provider))
    }
}