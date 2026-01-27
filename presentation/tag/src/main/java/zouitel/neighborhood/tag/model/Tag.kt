package zouitel.neighborhood.tag.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Tag(
     val id: Long = 0L,
     val label: String = "",
     val color: Int? = null
)