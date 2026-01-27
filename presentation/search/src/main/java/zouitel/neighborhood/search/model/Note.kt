package zouitel.neighborhood.search.model

import androidx.annotation.Keep

@Keep
data class Note(
    val data: Data,
    val tags: List<Tag>
)
