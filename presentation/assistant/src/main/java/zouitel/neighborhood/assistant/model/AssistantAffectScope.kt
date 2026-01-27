package zouitel.neighborhood.assistant.model

import kotlinx.serialization.Serializable

@Serializable
enum class AssistantAffectScope {
    USER,
    ASSISTANT;
}