package zouitel.neighborhood.repository.model

import androidx.annotation.Keep
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_BOOLEAN
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_INT
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT

@Keep
data class Data(
    val uid: String,
    val userUid: String,
    val title: String,
    val position: Int,
    val priority: Int,
    val background: Int?,
    val textColor: Int?,
    val date: String,
    val lastEdit: String,
    val archived: Boolean,
    val blocks: String,
    val medias: String,
    val audios: String,
    val reminders: String
)
