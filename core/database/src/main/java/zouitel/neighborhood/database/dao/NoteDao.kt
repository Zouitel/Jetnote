package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import zouitel.neighborhood.database.model.Data.Companion.TABLE_NAME
import zouitel.neighborhood.database.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @get: Query("SELECT * FROM $TABLE_NAME WHERE DELETED_AT IS NULL")
    val observeAll: Flow<List<Note>>

    @get: Query("SELECT * FROM $TABLE_NAME WHERE DELETED_AT IS NULL")
    val getAll: List<Note>

    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY POSITION ASC")
    fun observeByDefault(userUid: String): Flow<List<Note>>

    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY DATE DESC")
    fun observeByNewest(userUid: String): Flow<List<Note>>

    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY DATE ASC")
    fun observeByOldest(userUid: String): Flow<List<Note>>

    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY TITLE ASC")
    fun observeByName(userUid: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY CASE
            WHEN PRIORITY LIKE 4 THEN 1
            WHEN PRIORITY LIKE 3 THEN 2
            WHEN PRIORITY LIKE 2 THEN 3
            WHEN PRIORITY LIKE 1 THEN 4
            WHEN PRIORITY LIKE 0 THEN 5
            END
    """)
    fun observeByPriority(userUid: String): Flow<List<Note>>

    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 1 AND DELETED_AT IS NULL")
    fun observeArchives(userUid: String): Flow<List<Note>>

    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND DELETED_AT IS NOT NULL ORDER BY DELETED_AT DESC")
    fun observeTrash(userUid: String): Flow<List<Note>>

    @Query("DELETE FROM $TABLE_NAME WHERE TITLE = '' AND DELETED_AT IS NULL")
    suspend fun cleanup()
}
