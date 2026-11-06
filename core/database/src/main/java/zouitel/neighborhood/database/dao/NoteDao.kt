package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import zouitel.neighborhood.database.model.Data.Companion.TABLE_NAME
import zouitel.neighborhood.database.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid")
    fun observeAll(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid")
    suspend fun getAll(userUid: String): List<Note>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 ORDER BY POSITION ASC")
    fun observeByDefault(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 ORDER BY DATE DESC")
    fun observeByNewest(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 ORDER BY DATE ASC")
    fun observeByOldest(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 ORDER BY TITLE ASC")
    fun observeByName(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("""
        SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 ORDER BY CASE
            WHEN PRIORITY = 4 THEN 1
            WHEN PRIORITY = 3 THEN 2
            WHEN PRIORITY = 2 THEN 3
            WHEN PRIORITY = 1 THEN 4
            WHEN PRIORITY = 0 THEN 5
            ELSE 6
            END
    """)
    fun observeByPriority(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 1")
    fun observeArchives(userUid: String): Flow<List<Note>>

    @Query("DELETE FROM $TABLE_NAME WHERE USER_UID = :userUid AND TITLE = ''")
    suspend fun cleanup(userUid: String)
}