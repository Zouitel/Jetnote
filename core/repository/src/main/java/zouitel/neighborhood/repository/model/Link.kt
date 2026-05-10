package zouitel.neighborhood.repository.model

import androidx.annotation.Keep
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT

@Keep
data class Link(
     val url: String,
     val uid: String,
     val title: String,
     val description: String,
     val image: String,
     val icon: String,
     val domain: String
)