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
        dao.upsert(mapper.fromRepo(data))
    }

    override suspend fun insertAll(data: List<OutData>) {
        dao.insertAll(mapper.fromRepo(data))
    }

    override suspend fun combineUser(userUid: String?) {
        dao.combineUser(userUid)
    }

    override suspend fun archive(uid: String) {
        dao.archive(uid)
    }

    override suspend fun rollback(uid: String) {
        dao.rollback(uid)
    }

    override suspend fun delete(uid: String) {
        dao.delete(uid)
    }

    override suspend fun erase() {
        dao.erase()
    }

    override suspend fun sweepDrafts() {
        dao.sweepDrafts()
    }
}
