package zouitel.neighborhood.assistant.model

import android.net.Uri
import androidx.core.net.toUri
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.assistant.utils.toMessageNode
import kotlinx.serialization.Serializable
import kotlin.collections.flatMap
import kotlin.uuid.Uuid

@Serializable
data class Conversation(
    val id: Uuid = Uuid.Companion.random(),
    val assistantId: Short = 1,
    val title: String = "",
    val nodes: List<MessageNode> = emptyList(),
    val truncateIndex: Int = -1,
    val suggestions: List<String> = emptyList(),
    val isPinned: Boolean = false
) {
    val files: List<Uri>
        get() {
            val images = nodes
                .flatMap { node -> node.messages.flatMap { it.parts } }
                .filterIsInstance<UIMessagePart.Image>()
                .mapNotNull {
                    it.url.takeIf { it.startsWith("file://") }?.toUri()
                }
            val documents = nodes
                .flatMap { node -> node.messages.flatMap { it.parts } }
                .filterIsInstance<UIMessagePart.Document>()
                .mapNotNull {
                    it.url.takeIf { it.startsWith("file://") }?.toUri()
                }
            val videos = nodes
                .flatMap { node -> node.messages.flatMap { it.parts } }
                .filterIsInstance<UIMessagePart.Video>()
                .mapNotNull {
                    it.url.takeIf { it.startsWith("file://") }?.toUri()
                }
            val audios = nodes
                .flatMap { node -> node.messages.flatMap { it.parts } }
                .filterIsInstance<UIMessagePart.Audio>()
                .mapNotNull {
                    it.url.takeIf { it.startsWith("file://") }?.toUri()
                }
            return images + documents + videos + audios
        }

    /**
     *  当前选中的 message
     */
    val currentMessages
        get(): List<UIMessage> {
            return nodes.map { node -> node.messages[node.selectIndex] }
        }

    fun getMessageNodeByMessage(message: UIMessage): MessageNode? {
        return nodes.firstOrNull { node -> node.messages.contains(message) }
    }

    fun getMessageNodeByMessageId(messageId: Uuid): MessageNode? {
        return nodes.firstOrNull { node -> node.messages.any { it.id == messageId } }
    }

    fun updateCurrentMessages(messages: List<UIMessage>): Conversation {
        val newNodes = this.nodes.toMutableList()

        messages.forEachIndexed { index, message ->
            val node = newNodes
                .getOrElse(index) { message.toMessageNode() }

            val newMessages = node.messages.toMutableList()
            var newMessageIndex = node.selectIndex
            if (newMessages.any { it.id == message.id }) {
                newMessages[newMessages.indexOfFirst { it.id == message.id }] = message
            } else {
                newMessages.add(message)
                newMessageIndex = newMessages.lastIndex
            }

            val newNode = node.copy(
                messages = newMessages,
                selectIndex = newMessageIndex
            )

            // 更新newNodes
            if (index > newNodes.lastIndex) {
                newNodes.add(newNode)
            } else {
                newNodes[index] = newNode
            }
        }

        return this.copy(
            nodes = newNodes
        )
    }

    companion object {
        fun ofId(
            id: Uuid,
            assistantId: Short = 1,
            messages: List<MessageNode> = emptyList(),
        ) = Conversation(
            id = id,
            assistantId = assistantId,
            nodes = messages
        )
    }
}

