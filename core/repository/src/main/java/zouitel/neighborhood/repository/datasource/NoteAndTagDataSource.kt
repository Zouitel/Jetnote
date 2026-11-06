package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.NoteAndTag
import kotlinx.coroutines.flow.Flow

interface NoteAndTagDataSource {
    fun observeAll(userUid: String): Flow<List<NoteAndTag>>
    suspend fun getAll(userUid: String): List<NoteAndTag>
    suspend fun insert(noteAndTag: NoteAndTag, userUid: String)
    suspend fun insertAll(noteAndTags: List<NoteAndTag>, userUid: String)
    suspend fun delete(noteAndTag: NoteAndTag, userUid: String)
}