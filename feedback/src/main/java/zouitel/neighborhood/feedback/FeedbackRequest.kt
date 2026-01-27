package zouitel.neighborhood.feedback

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequest(
    val title: String,
    val body: String,
    val labels: Array<String>? = null,
    val assignees: List<String>? = null
)