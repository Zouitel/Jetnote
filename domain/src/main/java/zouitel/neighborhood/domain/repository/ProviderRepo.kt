package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Provider
import kotlinx.coroutines.flow.Flow

interface ProviderRepo {
    val observeAll: Flow<List<Provider>>

    suspend fun observeById(id: Long): Flow<Provider>

    suspend fun upsert(provider: Provider)
}