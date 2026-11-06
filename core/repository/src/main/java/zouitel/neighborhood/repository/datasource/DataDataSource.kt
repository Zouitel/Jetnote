package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Data

interface DataDataSource {
    suspend fun insert(data: Data)
    suspend fun insertAll(data: List<Data>)
    suspend fun archive(userUid: String, noteUid: String)
    suspend fun rollback(userUid: String, noteUid: String)
    suspend fun delete(userUid: String, noteUid: String)
    suspend fun erase(userUid: String)
    suspend fun sweepDrafts(userUid: String)
    suspend fun reorderNote(userUid: String, noteUid: String, position: Int)
    suspend fun reorderNotes(userUid: String, positions: Map<String, Int>)
}