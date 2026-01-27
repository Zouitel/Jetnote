package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.ModelDao
import zouitel.neighborhood.database.mapper.ModelMapper
import zouitel.neighborhood.repository.datasource.ModelDatasource
import zouitel.neighborhood.repository.model.Model
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModelDatasourceImpl(
    private val dao: ModelDao,
    private val mapper: ModelMapper
): ModelDatasource {
    override val observeAll: Flow<List<Model>>
        get() = dao.observeAll.map { mapper.toRepo(it) }

    override suspend fun observeById(id: String): Flow<Model?> {
        return dao.observeById(id).map { mapper.toRepo(it) }
    }

    override suspend fun observeByProviderId(providerId: Short): Flow<List<Model>> {
        return dao.observeByProviderId(providerId).map { mapper.toRepo(it) }
    }

    override suspend fun upsert(model: Model) {
        dao.upsert(mapper.fromRepo(model))
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}