package zouitel.neighborhood.feedback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadData(
    @SerialName("link")
    val link: String? = null
)