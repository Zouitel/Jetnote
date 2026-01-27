package zouitel.neighborhood.workspace.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Reminder(
    val id: Int = 0,
    val uid: String = "",
    val atTime: Long = 0L,
    val isPassed: Boolean = false
)