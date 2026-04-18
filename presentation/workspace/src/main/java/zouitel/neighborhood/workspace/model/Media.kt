package zouitel.neighborhood.workspace.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Keep
@Serializable
data class Media(
    val id: Long = Random.nextLong(),
    val uri: String = "",
    val isVideo: Boolean = false,
    val isDocument : Boolean = false,
    val type: MediaType = MediaType.IMAGE,
    val title: String? = DEFAULT_TXT,
    val color: Int = Color.Black.toArgb()
)