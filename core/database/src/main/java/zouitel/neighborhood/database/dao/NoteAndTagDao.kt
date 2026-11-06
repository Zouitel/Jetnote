package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import zouitel.neighborhood.database.model.NoteAndTag
import zouitel.neighborhood.database.model.NoteAndTag.Companion.TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAndTagDao {
    @Transaction
    @Query("""
        SELECT $TABLE_NAME.* FROM $TABLE_NAME
        INNER JOIN note_data_table ON $TABLE_NAME.uid = note_data_table.uuid
        WHERE note_data_table.user_uid = :userUid
    """)
    fun observeAll(userUid: String): Flow<List<NoteAndTag>>

    @Transaction
    @Query("""
        SELECT $TABLE_NAME.* FROM $TABLE_NAME
        INNER JOIN note_data_table ON $TABLE_NAME.uid = note_data_table.uuid
        WHERE note_data_table.user_uid = :userUid
    """)
    suspend fun getAll(userUid: String): List<NoteAndTag>

    @Query("""
        INSERT OR IGNORE INTO $TABLE_NAME (uid, id)
        SELECT :uid, :id
        WHERE EXISTS (
            SELECT 1 FROM note_data_table
            WHERE uuid = :uid AND user_uid = :userUid
        )
    """)
    suspend fun insertForUser(uid: String, id: Long, userUid: String)

    @Transaction
    suspend fun insertAllForUser(noteAndTags: List<NoteAndTag>, userUid: String) {
        noteAndTags.forEach { insertForUser(it.uid, it.id, userUid) }
    }

    @Query("""
        DELETE FROM $TABLE_NAME
        WHERE uid = :uid AND id = :id
        AND EXISTS (
            SELECT 1 FROM note_data_table
            WHERE uuid = :uid AND user_uid = :userUid
        )
    """)
    suspend fun deleteForUser(uid: String, id: Long, userUid: String)
}