package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteDataSource {
    val observeAll: Flow<List<Note>>
    val getAll: List<Note>
    fun observeByDefault(userUid: String): Flow<List<Note>>
    fun observeByName(userUid: String): Flow<List<Note>>
    fun observeByOldest(userUid: String): Flow<List<Note>>
    fun observeByNewest(userUid: String): Flow<List<Note>>
    fun observeByPriority(userUid: String): Flow<List<Note>>
    fun observeArchives(userUid: String): Flow<List<Note>>
    fun observeTrash(userUid: String): Flow<List<Note>>
    suspend fun cleanup()
}
