package zouitel.neighborhood.repository.model

data class Conversation(
    val uid: String,
    val title: String,
    val nodes: String,
    val truncateIndex: Int,
    val suggestions: String,
    val isPinned: Boolean
)

