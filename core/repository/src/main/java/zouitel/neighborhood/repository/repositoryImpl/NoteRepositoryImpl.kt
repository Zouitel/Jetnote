package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Note as OutNote
import zouitel.neighborhood.domain.repository.NoteRepository
import zouitel.neighborhood.repository.datasource.NoteDataSource
import zouitel.neighborhood.repository.mapper.NoteMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val dataSource: NoteDataSource,
    private val mapper: NoteMapper
): NoteRepository {
    override fun observeAll(userUid: String): Flow<List<OutNote>> =
        dataSource.observeAll(userUid).map { mapper.toDomain(it) }

    override suspend fun getAll(userUid: String): List<OutNote> =
        dataSource.getAll(userUid).map { mapper.toDomain(it) }

    override fun observeByDefault(userUid: String): Flow<List<OutNote>> =
        dataSource.observeByDefault(userUid).map { mapper.toDomain(it) }

    override fun observeByName(userUid: String): Flow<List<OutNote>> =
        dataSource.observeByName(userUid).map { mapper.toDomain(it) }

    override fun observeByOldest(userUid: String): Flow<List<OutNote>> =
        dataSource.observeByOldest(userUid).map { mapper.toDomain(it) }

    override fun observeByNewest(userUid: String): Flow<List<OutNote>> =
        dataSource.observeByNewest(userUid).map { mapper.toDomain(it) }

    override fun observeByPriority(userUid: String): Flow<List<OutNote>> =
        dataSource.observeByPriority(userUid).map { mapper.toDomain(it) }

    override fun observeArchives(userUid: String): Flow<List<OutNote>> =
        dataSource.observeArchives(userUid).map { mapper.toDomain(it) }

    override suspend fun cleanup(userUid: String) {
        dataSource.cleanup(userUid)
    }
}