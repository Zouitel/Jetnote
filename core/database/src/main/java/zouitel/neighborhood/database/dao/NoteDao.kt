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
    @Query("SELECT * FROM $TABLE_NAME WHERE DELETED_AT IS NULL")
    fun observeAll(): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE DELETED_AT IS NULL")
    fun getAll(): List<Note>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY POSITION ASC")
    fun observeByDefault(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY DATE DESC")
    fun observeByNewest(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY DATE ASC")
    fun observeByOldest(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 0 AND DELETED_AT IS NULL ORDER BY TITLE ASC")
    fun observeByName(userUid: String): Flow<List<Note>>

    @Transaction
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

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 1 AND DELETED_AT IS NULL")
    fun observeArchives(userUid: String): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM $TABLE_NAME WHERE USER_UID = :userUid AND DELETED_AT IS NOT NULL ORDER BY DELETED_AT DESC")
    fun observeTrash(userUid: String): Flow<List<Note>>

    /**
     * Full-text search is performed by the FTS4 virtual table. Title matches
     * are promoted above body-only matches; dates provide stable tie-breaking.
     */
    @Transaction
    @Query("""
        SELECT * FROM $TABLE_NAME
        WHERE UUID IN (
            SELECT uid FROM NoteFts WHERE NoteFts MATCH :matchQuery
        )
        AND USER_UID = :userUid
        AND ARCHIVED = 0
        AND DELETED_AT IS NULL
        ORDER BY CASE
            WHEN UUID IN (
                SELECT uid FROM NoteFts WHERE NoteFts MATCH :titleQuery
            ) THEN 0 ELSE 1
        END, DATE DESC
    """)
    fun observeSearch(
        userUid: String,
        matchQuery: String,
        titleQuery: String
    ): Flow<List<Note>>

    @Query("DELETE FROM $TABLE_NAME WHERE TITLE = '' AND DELETED_AT IS NULL")
    suspend fun cleanup()
}
