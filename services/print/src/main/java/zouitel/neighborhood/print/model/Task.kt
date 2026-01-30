package zouitel.neighborhood.print.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Task(
    val id: Long = 0L,
    val item: String? = null,
    val isDone: Boolean = false
)
