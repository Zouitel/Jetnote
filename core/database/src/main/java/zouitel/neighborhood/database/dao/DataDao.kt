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

    @Query("UPDATE $TABLE_NAME SET USER_UID  = :userUid WHERE USER_UID = 'null'")
    suspend fun combineUser(userUid: String?)

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 1 WHERE UUID = :uid")
    suspend fun archive(uid: String)

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 0 WHERE UUID = :uid")
    suspend fun rollback(uid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE UUID = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE ARCHIVED = 1")
    suspend fun erase()

    @Query("DELETE FROM $TABLE_NAME WHERE UUID = ''")
    suspend fun sweepDrafts()

    @Query("UPDATE $TABLE_NAME SET position = :position WHERE UUID = :uid")
    suspend fun reorderNote(uid: String, position: Int)

    @Transaction
    suspend fun reorderNotes(positions: Map<String, Int>) {
        positions.forEach { (uid, pos) -> reorderNote(uid, pos) }
    }
}