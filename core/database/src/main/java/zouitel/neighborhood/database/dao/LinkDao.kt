package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import zouitel.neighborhood.database.model.Link
import zouitel.neighborhood.database.model.Link.Companion.TABLE_NAME
import zouitel.neighborhood.database.utils.Constants.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @get: Query("SELECT * FROM $TABLE_NAME")
    val observeAll: Flow<List<Link>>

    @get: Query("SELECT * FROM $TABLE_NAME")
    val getAll: List<Link>

    @Query("SELECT * FROM $TABLE_NAME WHERE $UUID = :uid")
    fun observeByUid(uid: String): Flow<List<Link>>

    @Upsert(entity = Link::class)
    suspend fun insert(link: Link)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(links: List<Link>)

    @Delete
    suspend fun delete(link: Link)

    @Query("DELETE FROM $TABLE_NAME WHERE $UUID NOT IN (SELECT $UUID FROM note_data_table)")
    suspend fun deleteDrafts()
}