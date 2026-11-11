package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import zouitel.neighborhood.database.model.Data
import zouitel.neighborhood.database.model.Data.Companion.TABLE_NAME

@Dao
interface DataDao {
    @Upsert
    suspend fun upsert(data: Data)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(data: List<Data>)

    @Query("UPDATE $TABLE_NAME SET USER_UID = :userUid WHERE USER_UID = 'null'")
    suspend fun combineUser(userUid: String?)

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 1 WHERE UUID = :uid AND DELETED_AT IS NULL")
    suspend fun archive(uid: String)

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 0 WHERE UUID = :uid AND DELETED_AT IS NULL")
    suspend fun rollback(uid: String)

    @Query("UPDATE $TABLE_NAME SET DELETED_AT = :deletedAt WHERE UUID = :uid AND DELETED_AT IS NULL")
    suspend fun moveToTrash(uid: String, deletedAt: Long)

    @Query("UPDATE $TABLE_NAME SET DELETED_AT = NULL WHERE UUID = :uid AND USER_UID = :userUid AND DELETED_AT IS NOT NULL")
    suspend fun restoreFromTrash(uid: String, userUid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE UUID = :uid AND USER_UID = :userUid AND DELETED_AT IS NOT NULL")
    suspend fun permanentDelete(uid: String, userUid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE DELETED_AT IS NOT NULL AND DELETED_AT <= :cutoff")
    suspend fun purgeExpired(cutoff: Long)

    @Query("DELETE FROM $TABLE_NAME WHERE DELETED_AT IS NOT NULL AND USER_UID = :userUid")
    suspend fun emptyTrash(userUid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE ARCHIVED = 1 AND DELETED_AT IS NULL")
    suspend fun erase()

    @Query("DELETE FROM $TABLE_NAME WHERE UUID = ''")
    suspend fun sweepDrafts()

    @Query("UPDATE $TABLE_NAME SET position = :position WHERE UUID = :uid AND DELETED_AT IS NULL")
    suspend fun reorderNote(uid: String, position: Int)

    @Transaction
    suspend fun reorderNotes(positions: Map<String, Int>) {
        positions.forEach { (uid, pos) -> reorderNote(uid, pos) }
    }
}
