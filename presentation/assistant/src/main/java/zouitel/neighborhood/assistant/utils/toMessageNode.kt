package zouitel.neighborhood.assistant.utils

import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.assistant.model.MessageNode

fun UIMessage.toMessageNode(): MessageNode {
    return MessageNode(
        messages = listOf(this),
        selectIndex = 0
    )
}