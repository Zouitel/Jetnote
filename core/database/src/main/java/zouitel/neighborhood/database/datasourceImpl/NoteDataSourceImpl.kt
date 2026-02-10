package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.NoteDao
import zouitel.neighborhood.database.mapper.NoteMapper
import zouitel.neighborhood.repository.datasource.NoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.repository.model.Note as OutNote

class NoteDataSourceImpl(
    private val dao: NoteDao,
    private val mapper: NoteMapper
): NoteDataSource {
    override val observeAll: Flow<List<OutNote>>
        get() = dao.observeAll.map { mapper.toRepo(it) }

    override val getAll: List<OutNote>
        get() = dao.getAll.map { mapper.toRepo(it) }

    override suspend fun observeByDefault(userUid: String): Flow<List<OutNote>> {
        return dao.observeByDefault(userUid).map { mapper.toRepo(it) }
    }

    override fun observeByName(userUid: String): Flow<List<OutNote>> {
        return dao.observeByName(userUid).map { mapper.toRepo(it) }
    }

    override fun observeByOldest(userUid: String): Flow<List<OutNote>> {
        return dao.observeByOldest(userUid).map { mapper.toRepo(it) }
    }

    override fun observeByNewest(userUid: String): Flow<List<OutNote>> {
        return dao.observeByNewest(userUid).map { mapper.toRepo(it) }
    }

    override fun observeByPriority(userUid: String): Flow<List<OutNote>> {
        return dao.observeByPriority(userUid).map { mapper.toRepo(it) }
    }

    override fun observeArchives(userUid: String): Flow<List<OutNote>> = dao.observeArchives(userUid).map { mapper.toRepo(it) }

    override suspend fun cleanup() {
        dao.cleanup()
    }
}