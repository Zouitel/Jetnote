package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.domain.model.Assistant as OutAssistant
import zouitel.neighborhood.repository.model.Assistant

class AssistantMapper {
    fun toDomain(assistant: Assistant?) = OutAssistant(
        id = assistant?.id ?: 1,
        currentModelId = assistant?.currentModelId,
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
        presetMessages = assistant?.presetMessages ?: "[]",
        quickMessages = assistant?.quickMessages ?: "[]",
        regexes = assistant?.regexes ?: "[]",
        thinkingBudget = assistant?.thinkingBudget,
        maxTokens = assistant?.maxTokens,
        customHeaders = assistant?.customHeaders ?: "[]",
        customBodies = assistant?.customBodies ?: "[]",
        mcpServers = assistant?.mcpServers ?: "[]",
        localTools = assistant?.localTools ?: "[]",
        background = assistant?.background,
        learningMode = assistant?.learningMode ?: false
    )


    fun fromDomain(assistant: OutAssistant) = Assistant(
        id = assistant.id,
        currentModelId = assistant.currentModelId,
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
        presetMessages = assistant.presetMessages,
        quickMessages = assistant.quickMessages,
        regexes = assistant.regexes,
        thinkingBudget = assistant.thinkingBudget,
        maxTokens = assistant.maxTokens,
        customHeaders = assistant.customHeaders,
        customBodies = assistant.customBodies,
        mcpServers = assistant.mcpServers,
        localTools = assistant.localTools,
        background = assistant.background,
        learningMode = assistant.learningMode
    )
}