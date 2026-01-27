package zouitel.neighborhood.domain.model

data class Assistant(
    val id: Short,
    val currentModelId: String?,
    val name: String,
    val useAssistantAvatar: Boolean,
    val systemPrompt: String,
    val temperature: Float?,
    val topP: Float?,
    val contextMessageSize: Int,
    val streamOutput: Boolean,
    val enableMemory: Boolean,
    val enableRecentChatsReference: Boolean,
    val messageTemplate: String,
    val presetMessages: String,
    val quickMessages: String,
    val regexes: String,
    val thinkingBudget: Int?,
    val maxTokens: Int?,
    val customHeaders: String,
    val customBodies: String,
    val mcpServers: String,
    val localTools: String,
    val background: String?,
    val learningMode: Boolean
)