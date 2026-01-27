package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import zouitel.neighborhood.database.model.NoteAndTag
import zouitel.neighborhood.database.model.NoteAndTag.Companion.TABLE_NAME
import zouitel.neighborhood.database.utils.Constants.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAndTagDao {

    @get: Query("SELECT * FROM $TABLE_NAME")
    val observeAll:Flow<List<NoteAndTag>>

    @get: Query("SELECT * FROM $TABLE_NAME")
    val getAll: List<NoteAndTag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(noteAndLabel: NoteAndTag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(noteAndLabels: List<NoteAndTag>)

    @Query("DELETE FROM $TABLE_NAME WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM $TABLE_NAME WHERE uid NOT IN (SELECT $UUID FROM note_data_table)")
    suspend fun deleteDrafts()
}