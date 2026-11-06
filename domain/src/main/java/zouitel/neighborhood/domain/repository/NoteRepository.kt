package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeAll(userUid: String): Flow<List<Note>>
    suspend fun getAll(userUid: String): List<Note>
    fun observeByDefault(userUid: String): Flow<List<Note>>
    fun observeByName(userUid: String): Flow<List<Note>>
    fun observeByOldest(userUid: String): Flow<List<Note>>
    fun observeByNewest(userUid: String): Flow<List<Note>>
    fun observeByPriority(userUid: String): Flow<List<Note>>
    fun observeArchives(userUid: String): Flow<List<Note>>
    suspend fun cleanup(userUid: String)
}