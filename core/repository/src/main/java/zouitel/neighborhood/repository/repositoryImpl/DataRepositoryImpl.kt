package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Data
import zouitel.neighborhood.domain.repository.DataRepository
import zouitel.neighborhood.repository.datasource.DataDataSource
import zouitel.neighborhood.repository.mapper.DataMapper

class DataRepositoryImpl(
    private val dataSource: DataDataSource,
    private val mapper: DataMapper
): DataRepository {
    override suspend fun insert(data: Data) {
        dataSource.insert(mapper.fromDomain(data))
    }

    override suspend fun insertAll(data: List<Data>) {
        dataSource.insertAll(mapper.fromDomain(data))
    }

    override suspend fun archive(userUid: String, noteUid: String) {
        dataSource.archive(userUid, noteUid)
    }

    override suspend fun rollback(userUid: String, noteUid: String) {
        dataSource.rollback(userUid, noteUid)
    }

    override suspend fun delete(userUid: String, noteUid: String) {
        dataSource.delete(userUid, noteUid)
    }

    override suspend fun erase(userUid: String) {
        dataSource.erase(userUid)
    }

    override suspend fun sweepDrafts(userUid: String) {
        dataSource.sweepDrafts(userUid)
    }

    override suspend fun reorderNote(userUid: String, noteUid: String, position: Int) {
        dataSource.reorderNote(userUid, noteUid, position)
    }

    override suspend fun reorderNotes(userUid: String, positions: Map<String, Int>) {
        dataSource.reorderNotes(userUid, positions)
    }
}