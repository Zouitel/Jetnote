package zouitel.neighborhood.feedback.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    @SerialName("data")
    val data: UploadData? = null,
    @SerialName("success")
    val success: Boolean? = null,
    @SerialName("status")
    val status: Int? = null
)