package zouitel.neighborhood.assistant.mapper

import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.assistant.ai.LocalToolOption
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.model.AssistantRegex
import zouitel.neighborhood.assistant.model.QuickMessage
import zouitel.neighborhood.assistant.utils.JsonInstant
import kotlin.uuid.Uuid
import zouitel.neighborhood.domain.model.Assistant as OutAssistant

class AssistantMapper {
    fun toDomain(assistant: Assistant): OutAssistant {
        val presetMessages = JsonInstant.encodeToString(assistant.presetMessages)
        val quickMessages = JsonInstant.encodeToString(assistant.quickMessages)
        val regexes = JsonInstant.encodeToString(assistant.regexes)
        val customHeaders = JsonInstant.encodeToString(assistant.customHeaders)
        val customBodies = JsonInstant.encodeToString(assistant.customBodies)
        val mcpServers = JsonInstant.encodeToString(assistant.mcpServers)
        val localTools = JsonInstant.encodeToString(assistant.localTools)
        return OutAssistant(
            id = assistant.id,
            currentModelId = assistant.currentModelUid,
            name = assistant.name,
            useAssistantAvatar = assistant.useAssistantAvatar,
            systemPrompt = assistant.systemPrompt,
            temperature = assistant.temperature,
            topP = assistant.topP,
            contextMessageSize = assistant.contextMessageSize,
            streamOutput = assistant.streamOutput,
            enableMemory = assistant.enableMemory,
            enableRecentChatsReference = assistant.enableRecentChatsReference,
            messageTemplate = assistant.messageTemplate,
            presetMessages = presetMessages,
            quickMessages = quickMessages,
            regexes = regexes,
            thinkingBudget = assistant.thinkingBudget,
            maxTokens = assistant.maxTokens,
            customHeaders = customHeaders,
            customBodies = customBodies,
            mcpServers = mcpServers,
            localTools = localTools,
            background = assistant.background,
            learningMode = assistant.learningMode
        )
    }

    fun fromDomain(assistant: OutAssistant?): Assistant? {
        val presetMessages = JsonInstant.decodeFromString<List<UIMessage>>(assistant?.presetMessages?:"[]")
        val quickMessages = JsonInstant.decodeFromString<List<QuickMessage>>(assistant?.quickMessages?:"[]")
        val regexes = JsonInstant.decodeFromString<List<AssistantRegex>>(assistant?.regexes?:"[]")
        val customHeaders = JsonInstant.decodeFromString<List<CustomHeader>>(assistant?.customHeaders?:"[]")
        val customBodies = JsonInstant.decodeFromString<List<CustomBody>>(assistant?.customBodies?:"[]")
        val mcpServers = JsonInstant.decodeFromString<Set<Uuid>>(assistant?.mcpServers?:"[]")
        val localTools = JsonInstant.decodeFromString<List<LocalToolOption>>(assistant?.localTools?:"[]")
        return Assistant(
            id = assistant?.id ?: 1,
            currentModelUid = assistant?.currentModelId,
            name = assistant?.name ?: "",
            useAssistantAvatar = assistant?.useAssistantAvatar ?: false,
            systemPrompt = assistant?.systemPrompt ?: "",
            temperature = assistant?.temperature,
            topP = assistant?.topP,
            contextMessageSize = assistant?.contextMessageSize ?: 64,
            streamOutput = assistant?.streamOutput ?: true,
            enableMemory = assistant?.enableMemory ?: false,
            enableRecentChatsReference = assistant?.enableRecentChatsReference ?: false,
            messageTemplate = assistant?.messageTemplate ?: "{{ message }}",
            presetMessages = presetMessages,
            quickMessages = quickMessages,
            regexes = regexes,
            thinkingBudget = assistant?.thinkingBudget,
            maxTokens = assistant?.maxTokens,
            customHeaders = customHeaders,
            customBodies = customBodies,
            mcpServers = mcpServers,
            localTools = localTools,
            background = assistant?.background ?: "",
            learningMode = assistant?.learningMode ?: false
        )
    }
}
