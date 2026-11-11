package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Data

interface DataDataSource {
    suspend fun insert(data: Data)
    suspend fun insertAll(data: List<Data>)
    suspend fun combineUser(userUid: String?)
    suspend fun archive(uid: String)
    suspend fun rollback(uid: String)
    suspend fun delete(uid: String, deletedAt: Long)
    suspend fun restoreFromTrash(uid: String)
    suspend fun permanentDelete(uid: String)
    suspend fun purgeExpired(cutoff: Long)
    suspend fun emptyTrash()
    suspend fun erase()
    suspend fun sweepDrafts()
    suspend fun reorderNote(noteUid: String, position: Int)
    suspend fun reorderNotes(positions: Map<String, Int>)
}
