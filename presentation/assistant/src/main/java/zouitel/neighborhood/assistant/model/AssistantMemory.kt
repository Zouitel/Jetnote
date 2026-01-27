package zouitel.neighborhood.assistant.model

import kotlinx.serialization.Serializable

@Serializable
data class AssistantMemory(
    val id: Int,
    val content: String = "",
)