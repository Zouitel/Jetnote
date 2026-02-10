package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationDatasource {
    val observeAll: Flow<List<Conversation>>
    suspend fun observeById(id: String): Flow<Conversation>
    suspend fun upsert(conversation: Conversation)
    suspend fun delete(id: String)
    suspend fun cleanup()
}