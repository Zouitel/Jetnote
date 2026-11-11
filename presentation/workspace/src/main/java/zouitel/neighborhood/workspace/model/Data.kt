package zouitel.neighborhood.workspace.model

import androidx.annotation.Keep
import androidx.compose.runtime.Stable
import zouitel.neighborhood.audio.model.Audio
import kotlinx.serialization.Serializable
import zouitel.neighborhood.markdown.core.Block
import kotlin.time.Clock
import kotlin.time.Instant

@Stable
@Keep
@Serializable
data class Data (
    val uid: String = "",
    val userUid: String = "null",
    val title: String = "",
    val position: Int = 0,
    val priority: Int = 0,
    val background: Int? = null,
    val textColor: Int? = null,
    val date: Instant = Clock.System.now(),
    val lastEdit: Instant = Clock.System.now(),
    val archived: Boolean = false,
    val blocks: List<Block> = emptyList(),
    val medias: List<Media> = emptyList(),
    val audios: List<Audio> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val deletedAt: Long? = null
)
