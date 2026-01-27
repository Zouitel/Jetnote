package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.ProviderDao
import zouitel.neighborhood.database.mapper.ProviderMapper
import zouitel.neighborhood.repository.datasource.ProviderDatasource
import zouitel.neighborhood.repository.model.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProviderDatasourceImpl(
    private val dao: ProviderDao,
    private val mapper: ProviderMapper
): ProviderDatasource {
    override val observeAll: Flow<List<Provider>>
        get() = dao.observeAll.map { mapper.toRepo(it) }

    override suspend fun observeById(id: Long): Flow<Provider> {
        return dao.observeById(id).map { mapper.toRepo(it) }
    }

    override suspend fun upsert(provider: Provider) {
        dao.upsert(mapper.fromRepo(provider))
    }
}