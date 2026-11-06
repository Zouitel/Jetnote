package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.NoteAndTag
import kotlinx.coroutines.flow.Flow

interface NoteAndTagDataSource {
    val observeAll: Flow<List<NoteAndTag>>

    val getAll: List<NoteAndTag>

    suspend fun insert(noteAndTag: NoteAndTag)

    suspend fun insertAll(noteAndTags: List<NoteAndTag>)

    suspend fun delete(noteAndTag: NoteAndTag)

    suspend fun deleteDrafts()
}
