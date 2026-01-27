package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Provider
import kotlinx.coroutines.flow.Flow

interface ProviderDatasource {
    val observeAll: Flow<List<Provider>>

    suspend fun observeById(id: Long): Flow<Provider>

    suspend fun upsert(provider: Provider)
}