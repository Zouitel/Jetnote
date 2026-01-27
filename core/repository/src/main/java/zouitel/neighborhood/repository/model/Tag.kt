package zouitel.neighborhood.repository.model

import androidx.annotation.Keep
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_LONG
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT

@Keep
data class Tag(
     val id: Long = DEFAULT_LONG,
     val label: String = DEFAULT_TXT,
     val color: Int? = null
)