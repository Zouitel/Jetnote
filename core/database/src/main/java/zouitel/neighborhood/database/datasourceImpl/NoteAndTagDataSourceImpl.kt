package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.NoteAndTagDao
import zouitel.neighborhood.database.mapper.NoteAndTagMapper
import zouitel.neighborhood.repository.datasource.NoteAndTagDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.repository.model.NoteAndTag as OutNoteAndTag

class NoteAndTagDataSourceImpl(
    private val dao: NoteAndTagDao,
    private val mapper: NoteAndTagMapper
): NoteAndTagDataSource {
    override val observeAll: Flow<List<OutNoteAndTag>>
        get() = dao.observeAll.map { mapper.toRepo(it) }

    override val getAll: List<OutNoteAndTag>
        get() = dao.getAll.map { mapper.toRepo(it) }

    override suspend fun insert(noteAndTag: OutNoteAndTag) {
        dao.insert(mapper.fromRepo(noteAndTag))
    }

    override suspend fun insertAll(noteAndTags: List<OutNoteAndTag>) {
        dao.insertAll(mapper.fromRepo(noteAndTags))
    }

    override suspend fun delete(noteAndTag: OutNoteAndTag) {
        dao.delete(uid = noteAndTag.uid, id = noteAndTag.id)
    }

    override suspend fun deleteDrafts() {
        dao.deleteDrafts()
    }
}
