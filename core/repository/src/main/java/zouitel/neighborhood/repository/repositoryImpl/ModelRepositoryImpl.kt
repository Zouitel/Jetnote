package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Model
import zouitel.neighborhood.domain.repository.ModelRepo
import zouitel.neighborhood.repository.datasource.ModelDatasource
import zouitel.neighborhood.repository.mapper.ModelMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModelRepositoryImpl(
    private val datasource: ModelDatasource,
    private val mapper: ModelMapper
): ModelRepo {
    override val observeAll: Flow<List<Model>>
        get() = datasource.observeAll.map { mapper.toDomain(it) }

    override suspend fun observeById(id: String): Flow<Model?> {
        return datasource.observeById(id).map { mapper.toDomain(it) }
    }

    override suspend fun observeByProviderId(providerId: Short): Flow<List<Model>> {
        return datasource.observeByProviderId(providerId).map { mapper.toDomain(it) }
    }

    override suspend fun upsert(model: Model) {
        datasource.upsert(mapper.fromDomain(model))
    }

    override suspend fun delete(id: String) {
        datasource.delete(id)
    }
}