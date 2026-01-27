package zouitel.neighborhood.assistant.model

import kotlinx.serialization.Serializable

@Serializable
data class AssistantRegex(
    val id: Short,
    val name: String = "",
    val enabled: Boolean = true,
    val findRegex: String = "",
    val replaceString: String = "",
    val affectingScope: Set<AssistantAffectScope> = setOf(),
    val visualOnly: Boolean = false
)