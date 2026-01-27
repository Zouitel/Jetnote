package zouitel.neighborhood.repository.model

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
    val tasks: String,
    val medias: String,
    val audios: String,
    val reminders: String
)
