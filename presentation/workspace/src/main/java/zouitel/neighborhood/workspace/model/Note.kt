package zouitel.neighborhood.workspace.model

import androidx.annotation.Keep
import androidx.compose.runtime.Stable
import zouitel.neighborhood.tag.model.Tag
import kotlinx.serialization.Serializable

@Stable
@Keep
@Serializable
data class Note(
    val data: Data,
    val tags: List<Tag>
)
