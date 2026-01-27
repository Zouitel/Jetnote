package zouitel.neighborhood.widget.model

import androidx.annotation.Keep
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.tag.model.Tag

@Keep
data class WidgetNote(
    val data: Data,
    val tags: List<Tag>
)