package zouitel.neighborhood.search.model

import androidx.annotation.Keep

@Keep
data class Tag(
     val id: Long = 0L,
     val label: String = "",
     val color: Int? = null
)