package zouitel.neighborhood.assistant.model

import androidx.compose.runtime.Stable
import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.ai.ui.UIMessage
import kotlinx.serialization.Serializable
import zouitel.neighborhood.assistant.ai.LocalToolOption
import kotlin.uuid.Uuid

@Stable
@Serializable
data class Assistant(
    val id: Short = 1,
    val currentModelUid: String? = null,
    val name: String = "",
    val useAssistantAvatar: Boolean = false,
    val systemPrompt: String = "",
    val temperature: Float? = null,
    val topP: Float? = null,
    val contextMessageSize: Int = 100,
    val streamOutput: Boolean = true,
    val enableMemory: Boolean = false,
    val enableRecentChatsReference: Boolean = false,
    val messageTemplate: String = "{{ message }}",
    val presetMessages: List<UIMessage> = emptyList(),
    val quickMessages: List<QuickMessage> = emptyList(),
    val regexes: List<AssistantRegex> = emptyList(),
    val thinkingBudget: Int? = null,
    val maxTokens: Int? = null,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val mcpServers: Set<Uuid> = emptySet(),
    val localTools: List<LocalToolOption> = emptyList(),
    val background: String? = null,
    val learningMode: Boolean = false
)







