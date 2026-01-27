package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Model
import kotlinx.coroutines.flow.Flow

interface ModelRepo {
    val observeAll: Flow<List<Model>>
    suspend fun observeById(id: String): Flow<Model?>
    suspend fun observeByProviderId(providerId: Short): Flow<List<Model>>
    suspend fun upsert(model: Model)
    suspend fun delete(id: String)
}