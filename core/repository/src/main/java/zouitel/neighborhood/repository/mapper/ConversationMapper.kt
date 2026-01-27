package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Conversation
import zouitel.neighborhood.domain.model.Conversation as OutConversation

class ConversationMapper {
    fun toDomain(conversation: List<Conversation>) = conversation.map { toDomain(it) }

    fun toDomain(conversation: Conversation) = OutConversation(
        id = conversation.uid,
        title = conversation.title,
        nodes = conversation.nodes,
        truncateIndex = conversation.truncateIndex,
        suggestions = conversation.suggestions,
        isPinned = conversation.isPinned
    )

    fun fromDomain(conversation: OutConversation) = Conversation(
        uid = conversation.id,
        title = conversation.title,
        nodes = conversation.nodes,
        truncateIndex = conversation.truncateIndex,
        suggestions = conversation.suggestions,
        isPinned = conversation.isPinned
    )
}