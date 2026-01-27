package zouitel.neighborhood.workspace.model

import androidx.annotation.Keep
import zouitel.neighborhood.tag.model.Tag
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Note(
    val data: Data,
    val tags: List<Tag>
)
