package zouitel.neighborhood.domain.model

import androidx.annotation.Keep
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_BOOLEAN
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_INT
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT

@Keep
data class Data(
    val uid: String = DEFAULT_TXT,
    val userUid: String = DEFAULT_TXT,
    val title: String = DEFAULT_TXT,
    val description: String = DEFAULT_TXT,
    val priority: Int = DEFAULT_INT,
    val background: Int? = null,
    val textColor: Int? = null,
    val date: String = DEFAULT_TXT,
    val archived: Boolean = DEFAULT_BOOLEAN,
    val tasks: String = DEFAULT_TXT,
    val medias: String = DEFAULT_TXT,
    val audios: String = DEFAULT_TXT,
    val reminders: String = DEFAULT_TXT
)
