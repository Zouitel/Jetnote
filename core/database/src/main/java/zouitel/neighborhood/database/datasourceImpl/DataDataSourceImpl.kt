package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.DataDao
import zouitel.neighborhood.database.mapper.DataMapper
import zouitel.neighborhood.repository.datasource.DataDataSource
import zouitel.neighborhood.repository.model.Data as OutData

class DataDataSourceImpl(
    private val dao: DataDao,
    private val mapper: DataMapper
): DataDataSource {
    override suspend fun insert(data: OutData) {
        dao.upsertForUser(mapper.fromRepo(data))
    }

    override suspend fun insertAll(data: List<OutData>) {
        require(data.all { it.uid.isNotBlank() && it.userUid.isNotBlank() && it.userUid != "null" }) {
            "Every imported note must have an ID and belong to an authenticated user"
        }
        dao.insertAll(mapper.fromRepo(data))
    }

    override suspend fun archive(userUid: String, noteUid: String) {
        dao.archive(userUid, noteUid)
    }

    override suspend fun rollback(userUid: String, noteUid: String) {
        dao.rollback(userUid, noteUid)
    }

    override suspend fun delete(userUid: String, noteUid: String) {
        dao.delete(userUid, noteUid)
    }

    override suspend fun erase(userUid: String) {
        dao.erase(userUid)
    }

    override suspend fun sweepDrafts(userUid: String) {
        dao.sweepDrafts(userUid)
    }

    override suspend fun reorderNote(userUid: String, noteUid: String, position: Int) {
        dao.reorderNote(userUid, noteUid, position)
    }

    override suspend fun reorderNotes(userUid: String, positions: Map<String, Int>) {
        dao.reorderNotes(userUid, positions)
    }
}