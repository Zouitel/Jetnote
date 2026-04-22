package zouitel.neighborhood.feedback.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadData(
    @SerialName("link")
    val link: String? = null
)