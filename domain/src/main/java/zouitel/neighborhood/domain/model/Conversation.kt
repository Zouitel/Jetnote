package zouitel.neighborhood.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val nodes: String,
    val truncateIndex: Int,
    val suggestions: String,
    val isPinned: Boolean
)
