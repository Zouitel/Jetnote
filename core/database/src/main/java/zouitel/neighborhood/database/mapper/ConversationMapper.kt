package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.Conversation
import zouitel.neighborhood.repository.model.Conversation as OutConversation

class ConversationMapper {
    fun toRepo(conversation: List<Conversation>) = conversation.map { toRepo(it) }

    fun toRepo(conversation: Conversation) = OutConversation(
        uid = conversation.uid,
        title = conversation.title,
        nodes = conversation.nodes,
        truncateIndex = conversation.truncateIndex,
        suggestions = conversation.suggestions,
        isPinned = conversation.isPinned
    )

    fun fromRepo(conversation: OutConversation) = Conversation(
        uid = conversation.uid,
        title = conversation.title,
        nodes = conversation.nodes,
        truncateIndex = conversation.truncateIndex,
        suggestions = conversation.suggestions,
        isPinned = conversation.isPinned
    )
}