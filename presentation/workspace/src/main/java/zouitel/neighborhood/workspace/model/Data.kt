package zouitel.neighborhood.workspace.model

import androidx.annotation.Keep
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_BOOLEAN
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_INT
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Keep
@Serializable
data class Data (
    val uid: String = DEFAULT_TXT,
    val userUid: String = "null",
    val title: String = DEFAULT_TXT,
    val description: String = DEFAULT_TXT,
    val priority: Int = DEFAULT_INT,
    val background: Int? = null,
    val textColor: Int? = null,
    val date: Instant = Clock.System.now(),
    val lastEdit: Instant = Clock.System.now(),
    val archived: Boolean = DEFAULT_BOOLEAN,
    val tasks: List<Task> = emptyList(),
    val medias: List<Media> = emptyList(),
    val audios: List<Audio> = emptyList(),
    val reminders: List<Reminder> = emptyList()
)