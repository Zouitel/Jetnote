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
    override val observeAll: Flow<List<OutNoteAndTag>>
        get() = dataSource.observeAll.map { mapper.toDomain(it) }

    override val getAll: List<OutNoteAndTag>
        get() = dataSource.getAll.map { mapper.toDomain(it) }

    override suspend fun insert(noteAndTag: OutNoteAndTag) {
        dataSource.insert(mapper.fromDomain(noteAndTag))
    }

    override suspend fun insertAll(noteAndTags: List<OutNoteAndTag>) {
        dataSource.insertAll(mapper.fromDomain(noteAndTags))
    }

    override suspend fun delete(noteAndTag: OutNoteAndTag) {
        dataSource.delete(mapper.fromDomain(noteAndTag))
    }

    override suspend fun deleteDrafts() {
        dataSource.deleteDrafts()
    }
}
