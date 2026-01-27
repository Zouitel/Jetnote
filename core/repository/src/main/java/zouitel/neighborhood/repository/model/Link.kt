package zouitel.neighborhood.repository.model

import androidx.annotation.Keep
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT

@Keep
data class Link(
     val url: String = DEFAULT_TXT,
     val uid: String = DEFAULT_TXT,
     val title: String = DEFAULT_TXT,
     val description: String = DEFAULT_TXT,
     val image: String = DEFAULT_TXT,
     val icon: String = DEFAULT_TXT,
     val domain: String = DEFAULT_TXT
)