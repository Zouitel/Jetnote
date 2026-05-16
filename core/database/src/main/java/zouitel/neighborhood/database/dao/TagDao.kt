package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import zouitel.neighborhood.database.model.Tag
import zouitel.neighborhood.database.model.Tag.Companion.TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @get:Query("SELECT * FROM $TABLE_NAME")
    val observeAll: Flow<List<Tag>>

    @get:Query("SELECT * FROM $TABLE_NAME")
    val getAll: List<Tag>

    @Upsert
    suspend fun insert(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<Tag>)

    @Query("DELETE FROM $TABLE_NAME WHERE ID = :id")
    suspend fun delete(id: Long)
}