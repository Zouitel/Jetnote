package zouitel.neighborhood.jotting.model

import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_BOOLEAN
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_INT
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.Task
import kotlin.time.Clock
import kotlin.time.Instant

data class QuickData(
    var uid: String = DEFAULT_TXT,
    var title: String = DEFAULT_TXT,
    var description: String = DEFAULT_TXT,
    var priority: Int = DEFAULT_INT,
    var background: Int = DEFAULT_INT,
    var textColor: Int = DEFAULT_INT,
    var date: Instant = Clock.System.now(),
    var archived: Boolean = DEFAULT_BOOLEAN,
    val blocks: List<Block> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val medias: List<Media> = emptyList()
)
