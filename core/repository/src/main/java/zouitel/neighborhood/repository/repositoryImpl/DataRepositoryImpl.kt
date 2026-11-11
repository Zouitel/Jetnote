package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Data
import zouitel.neighborhood.domain.repository.DataRepository
import zouitel.neighborhood.repository.datasource.DataDataSource
import zouitel.neighborhood.repository.mapper.DataMapper

class DataRepositoryImpl(
    private val dataSource: DataDataSource,
    private val mapper: DataMapper
): DataRepository {
    override suspend fun insert(data: Data) = dataSource.insert(mapper.fromDomain(data))
    override suspend fun insertAll(data: List<Data>) = dataSource.insertAll(mapper.fromDomain(data))
    override suspend fun combineUser(userUid: String?) = dataSource.combineUser(userUid)
    override suspend fun archive(uid: String) = dataSource.archive(uid)
    override suspend fun rollback(uid: String) = dataSource.rollback(uid)
    override suspend fun delete(uid: String, deletedAt: Long) = dataSource.delete(uid, deletedAt)
    override suspend fun restoreFromTrash(uid: String, userUid: String) = dataSource.restoreFromTrash(uid, userUid)
    override suspend fun permanentDelete(uid: String, userUid: String) = dataSource.permanentDelete(uid, userUid)
    override suspend fun purgeExpired(cutoff: Long) = dataSource.purgeExpired(cutoff)
    override suspend fun emptyTrash(userUid: String) = dataSource.emptyTrash(userUid)
    override suspend fun erase() = dataSource.erase()
    override suspend fun sweepDrafts() = dataSource.sweepDrafts()
    override suspend fun reorderNote(noteUid: String, position: Int) = dataSource.reorderNote(noteUid, position)
    override suspend fun reorderNotes(positions: Map<String, Int>) = dataSource.reorderNotes(positions)
}
