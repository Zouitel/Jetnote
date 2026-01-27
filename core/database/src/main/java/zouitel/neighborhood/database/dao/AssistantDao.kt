package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import zouitel.neighborhood.database.model.Assistant
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    @get: Query("SELECT * FROM ASSISTANT_TABLE WHERE ID = 1")
    val observe: Flow<Assistant?>

    @Upsert(entity = Assistant::class)
    fun upsert(assistant: Assistant)
}