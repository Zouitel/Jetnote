package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import zouitel.neighborhood.database.model.Data
import zouitel.neighborhood.database.model.Data.Companion.TABLE_NAME
import zouitel.neighborhood.database.utils.Constants.USER_UID
import zouitel.neighborhood.database.utils.Constants.UUID


@Dao
interface DataDao {
    @Upsert
    suspend fun upsert(data: Data)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(data: List<Data>)

    @Query("UPDATE $TABLE_NAME SET USER_UNIQUE_IDENTIFIER = :userUid WHERE $USER_UID = 'null'")
    suspend fun combineUser(userUid: String?)

    @Query("UPDATE $TABLE_NAME SET DATE = datetime('now') WHERE $UUID = :uid")
    suspend fun lastEdit(uid: String)

    @Transaction
    suspend fun insert(data: Data) {
        upsert(data)
        lastEdit(data.uid)
    }

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 1 WHERE $UUID = :uid")
    suspend fun archive(uid: String)

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 0 WHERE $UUID = :uid")
    suspend fun rollback(uid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE $UUID = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE ARCHIVED = 1")
    suspend fun erase()

    @Query("DELETE FROM $TABLE_NAME WHERE USER_UNIQUE_IDENTIFIER = ''")
    suspend fun sweepDrafts()
}