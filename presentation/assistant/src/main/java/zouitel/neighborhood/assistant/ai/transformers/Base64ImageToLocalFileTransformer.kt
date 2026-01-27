package zouitel.neighborhood.assistant.ai.transformers

import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.assistant.utils.convertBase64ImagePartToLocalFile

object Base64ImageToLocalFileTransformer : OutputMessageTransformer {
    override suspend fun onGenerationFinish(
        ctx: TransformerContext,
        messages: List<UIMessage>
    ): List<UIMessage> {
        return messages.map { message ->
            ctx.context.convertBase64ImagePartToLocalFile(message)
        }
    }
}
