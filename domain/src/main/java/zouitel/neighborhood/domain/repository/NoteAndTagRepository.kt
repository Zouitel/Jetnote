package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.NoteAndTag
import kotlinx.coroutines.flow.Flow

interface NoteAndTagRepository {
    val observeAll: Flow<List<NoteAndTag>>

    val getAll: List<NoteAndTag>

    suspend fun insert(noteAndTag: NoteAndTag)

    suspend fun insertAll(noteAndTags: List<NoteAndTag>)

    suspend fun delete(id: Long)

    suspend fun deleteDrafts()
}