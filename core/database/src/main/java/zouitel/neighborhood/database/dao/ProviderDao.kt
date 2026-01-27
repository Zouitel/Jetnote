package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import zouitel.neighborhood.database.model.Provider
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @get: Query("SELECT * FROM PROVIDER_TABLE")
    val observeAll: Flow<List<Provider>>

    @Query("SELECT * FROM PROVIDER_TABLE WHERE ID = :id")
    fun observeById(id: Long): Flow<Provider>

    @Upsert(Provider::class)
    suspend fun upsert(provider: Provider)
}