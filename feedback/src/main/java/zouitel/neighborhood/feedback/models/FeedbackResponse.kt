package zouitel.neighborhood.feedback.models

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackResponse(
    val id: Long,
    val number: Int,
    val title: String,
    val html_url: String,
    val state: String
)