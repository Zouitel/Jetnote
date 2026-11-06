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
    override fun observeAll(userUid: String): Flow<List<OutNoteAndTag>> =
        dao.observeAll(userUid).map { mapper.toRepo(it) }

    override suspend fun getAll(userUid: String): List<OutNoteAndTag> =
        dao.getAll(userUid).map { mapper.toRepo(it) }

    override suspend fun insert(noteAndTag: OutNoteAndTag, userUid: String) {
        dao.insertForUser(noteAndTag.uid, noteAndTag.id, userUid)
    }

    override suspend fun insertAll(noteAndTags: List<OutNoteAndTag>, userUid: String) {
        dao.insertAllForUser(mapper.fromRepo(noteAndTags), userUid)
    }

    override suspend fun delete(noteAndTag: OutNoteAndTag, userUid: String) {
        dao.deleteForUser(noteAndTag.uid, noteAndTag.id, userUid)
    }
}