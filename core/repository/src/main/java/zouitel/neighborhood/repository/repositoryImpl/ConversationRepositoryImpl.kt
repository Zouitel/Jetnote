package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.model.Conversation as OutConversation
import zouitel.neighborhood.domain.repository.ConversationRepo
import zouitel.neighborhood.repository.datasource.ConversationDatasource
import zouitel.neighborhood.repository.mapper.ConversationMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationRepositoryImpl(
    private val datasource: ConversationDatasource,
    private val mapper: ConversationMapper
): ConversationRepo {
    override val observeAll: Flow<List<OutConversation>>
        get() = datasource.observeAll.map { mapper.toDomain(it) }

    override suspend fun observeById(id: String): Flow<OutConversation> {
        return datasource.observeById(id).map { mapper.toDomain(it) }
    }

    override suspend fun upsert(conversation: OutConversation) {
        datasource.upsert(mapper.fromDomain(conversation))
    }

    override suspend fun delete(id: String) {
        datasource.delete(id)
    }
}