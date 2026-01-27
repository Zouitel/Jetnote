package zouitel.neighborhood.assistant.mapper

import zouitel.neighborhood.assistant.model.Conversation
import zouitel.neighborhood.assistant.model.MessageNode
import zouitel.neighborhood.assistant.utils.JsonInstant
import kotlin.collections.isNotEmpty
import kotlin.uuid.Uuid
import zouitel.neighborhood.domain.model.Conversation as OutConversation


class ConversationMapper {
    fun fromDomain(conversations: List<OutConversation>) = conversations.map { fromDomain(it) }

    fun fromDomain(conversation: OutConversation): Conversation {
        val id = Uuid.parse(conversation.id)
        val nodes = JsonInstant.decodeFromString<List<MessageNode>>(conversation.nodes)
            .filter { it.messages.isNotEmpty() }
        val suggestions = JsonInstant.decodeFromString<List<String>>(conversation.suggestions)
       return Conversation(
            id = id,
            title = conversation.title,
            nodes = nodes,
            truncateIndex = conversation.truncateIndex,
            suggestions = suggestions,
            isPinned = conversation.isPinned
        )
    }
    fun toDomain(conversation: Conversation): OutConversation {
        val id = conversation.id.toString()
        val nodes = JsonInstant.encodeToString(conversation.nodes)
        val suggestions = JsonInstant.encodeToString(conversation.suggestions)
       return OutConversation(
            id = id,
            title = conversation.title,
            nodes = nodes,
            truncateIndex = conversation.truncateIndex,
            suggestions = suggestions,
            isPinned = conversation.isPinned
        )
    }
}