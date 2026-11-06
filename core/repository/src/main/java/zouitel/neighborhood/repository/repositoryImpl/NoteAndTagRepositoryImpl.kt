package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.repository.NoteAndTagRepository
import zouitel.neighborhood.repository.datasource.NoteAndTagDataSource
import zouitel.neighborhood.repository.mapper.NoteAndTagMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.domain.model.NoteAndTag as OutNoteAndTag

class NoteAndTagRepositoryImpl(
    private val dataSource: NoteAndTagDataSource,
    private val mapper: NoteAndTagMapper
): NoteAndTagRepository {
    override fun observeAll(userUid: String): Flow<List<OutNoteAndTag>> =
        dataSource.observeAll(userUid).map { mapper.toDomain(it) }

    override suspend fun getAll(userUid: String): List<OutNoteAndTag> =
        dataSource.getAll(userUid).map { mapper.toDomain(it) }

    override suspend fun insert(noteAndTag: OutNoteAndTag, userUid: String) {
        dataSource.insert(mapper.fromDomain(noteAndTag), userUid)
    }

    override suspend fun insertAll(noteAndTags: List<OutNoteAndTag>, userUid: String) {
        dataSource.insertAll(mapper.fromDomain(noteAndTags), userUid)
    }

    override suspend fun delete(noteAndTag: OutNoteAndTag, userUid: String) {
        dataSource.delete(mapper.fromDomain(noteAndTag), userUid)
    }
}