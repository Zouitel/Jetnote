package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import zouitel.neighborhood.database.model.NoteAndTag
import zouitel.neighborhood.database.model.NoteAndTag.Companion.TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAndTagDao {

    @get: Query("SELECT * FROM $TABLE_NAME")
    val observeAll: Flow<List<NoteAndTag>>

    @get: Query("SELECT * FROM $TABLE_NAME")
    val getAll: List<NoteAndTag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(noteAndTag: NoteAndTag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(noteAndTags: List<NoteAndTag>)

    @Query("DELETE FROM $TABLE_NAME WHERE uid = :uid AND id = :id")
    suspend fun delete(uid: String, id: Long)

    @Query("DELETE FROM $TABLE_NAME WHERE uid NOT IN (SELECT UUID FROM note_data_table)")
    suspend fun deleteDrafts()
}
