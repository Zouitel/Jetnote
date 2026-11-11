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
    override val observeAll: Flow<List<OutNote>>
        get() = dataSource.observeAll.map { mapper.toDomain(it) }
    override val getAll: List<OutNote>
        get() = dataSource.getAll.map { mapper.toDomain(it) }
    override fun observeByDefault(userUid: String) =
        dataSource.observeByDefault(userUid).map { mapper.toDomain(it) }
    override fun observeByName(userUid: String) =
        dataSource.observeByName(userUid).map { mapper.toDomain(it) }
    override fun observeByOldest(userUid: String) =
        dataSource.observeByOldest(userUid).map { mapper.toDomain(it) }
    override fun observeByNewest(userUid: String) =
        dataSource.observeByNewest(userUid).map { mapper.toDomain(it) }
    override fun observeByPriority(userUid: String) =
        dataSource.observeByPriority(userUid).map { mapper.toDomain(it) }
    override fun observeArchives(userUid: String) =
        dataSource.observeArchives(userUid).map { mapper.toDomain(it) }
    override fun observeTrash(userUid: String) =
        dataSource.observeTrash(userUid).map { mapper.toDomain(it) }
    override suspend fun cleanup() = dataSource.cleanup()
}
