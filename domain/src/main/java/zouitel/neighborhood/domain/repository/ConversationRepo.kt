package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepo {
    val observeAll: Flow<List<Conversation>>
    suspend fun observeById(id: String): Flow<Conversation>
    suspend fun upsert(conversation: Conversation)
    suspend fun delete(id: String)
    suspend fun cleanup()
}