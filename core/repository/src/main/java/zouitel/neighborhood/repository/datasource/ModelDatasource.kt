package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Model
import kotlinx.coroutines.flow.Flow

interface ModelDatasource {
    val observeAll: Flow<List<Model>>
    suspend fun observeById(id: String): Flow<Model?>
    suspend fun observeByProviderId(providerId: Short): Flow<List<Model>>
    suspend fun upsert(model: Model)
    suspend fun delete(id: String)
}