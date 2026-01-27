package zouitel.neighborhood.tag.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class NoteAndTag(
    val uid: String,
    val id: Long
)
