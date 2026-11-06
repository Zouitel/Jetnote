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

    @Query("SELECT USER_UID FROM $TABLE_NAME WHERE UUID = :noteUid LIMIT 1")
    suspend fun ownerOf(noteUid: String): String?

    @Transaction
    suspend fun upsertForUser(data: Data) {
        require(data.uid.isNotBlank()) { "A note UUID is required" }
        require(data.userUid.isNotBlank() && data.userUid != "null") {
            "A note must belong to an authenticated user"
        }
        val existingOwner = ownerOf(data.uid)
        check(existingOwner == null || existingOwner == data.userUid) {
            "A note owned by another user cannot be overwritten"
        }
        upsert(data)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(data: List<Data>)

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 1 WHERE USER_UID = :userUid AND UUID = :noteUid")
    suspend fun archive(userUid: String, noteUid: String)

    @Query("UPDATE $TABLE_NAME SET ARCHIVED = 0 WHERE USER_UID = :userUid AND UUID = :noteUid")
    suspend fun rollback(userUid: String, noteUid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE USER_UID = :userUid AND UUID = :noteUid")
    suspend fun delete(userUid: String, noteUid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE USER_UID = :userUid AND ARCHIVED = 1")
    suspend fun erase(userUid: String)

    @Query("DELETE FROM $TABLE_NAME WHERE USER_UID = :userUid AND UUID = ''")
    suspend fun sweepDrafts(userUid: String)

    @Query("UPDATE $TABLE_NAME SET position = :position WHERE USER_UID = :userUid AND UUID = :noteUid")
    suspend fun reorderNote(userUid: String, noteUid: String, position: Int)

    @Transaction
    suspend fun reorderNotes(userUid: String, positions: Map<String, Int>) {
        positions.forEach { (noteUid, position) ->
            reorderNote(userUid, noteUid, position)
        }
    }
}