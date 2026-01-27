package zouitel.neighborhood.assistant.model

import kotlinx.serialization.Serializable

@Serializable
data class QuickMessage(
    val title: String = "",
    val content: String = ""
)
