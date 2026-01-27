package zouitel.neighborhood.assistant.ai.transformers

import zouitel.neighborhood.ai.core.MessageRole
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.assistant.model.AssistantAffectScope
import zouitel.neighborhood.assistant.utils.replaceRegexes
import org.koin.core.component.KoinComponent
import kotlin.time.ExperimentalTime

object RegexOutputTransformer : OutputMessageTransformer, KoinComponent {
    @OptIn(ExperimentalTime::class)
    override suspend fun visualTransform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val assistant = ctx.assistant
        if (assistant?.regexes?.isEmpty() == true) return messages // No regexes, return original messages
        return messages.map { message ->
            val scope = when (message.role) {
                MessageRole.ASSISTANT -> AssistantAffectScope.ASSISTANT
                else -> return@map message // Skip non-assistant messages
            }
            message.copy(
                parts = message.parts.map { part ->
                    when (part) {
                        is UIMessagePart.Text -> {
                            part.copy(text = part.text.replaceRegexes(assistant, scope, visual = false))
                        }

                        is UIMessagePart.Reasoning -> {
                            part.copy(reasoning = part.reasoning.replaceRegexes(assistant, scope, visual = false))
                        }

                        else -> part
                    }
                }
            )
        }
    }
}
