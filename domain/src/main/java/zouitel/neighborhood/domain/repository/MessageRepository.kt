package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {

    val observeAll: Flow<List<Message>>

    suspend fun insert(message: Message)
}