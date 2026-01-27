package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import zouitel.neighborhood.database.model.Model
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @get: Query("SELECT * FROM MODELS_TABLE")
    val observeAll: Flow<List<Model>>

    @Query("SELECT * FROM MODELS_TABLE WHERE uid = :uid")
    fun observeById(uid: String): Flow<Model?>

    @Query("SELECT * FROM MODELS_TABLE WHERE PROVIDERID = :id")
    fun observeByProviderId(id: Short): Flow<List<Model>>

    @Upsert(Model::class)
    suspend fun upsert(model: Model)

    @Query("DELETE FROM MODELS_TABLE WHERE modelId = :id")
    suspend fun delete(id: String)
}