package zouitel.neighborhood.bin.model

import androidx.annotation.Keep
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_BOOLEAN
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_LONG
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Audio(
    val id: Long = DEFAULT_LONG,
    val uid: String = DEFAULT_TXT,
    val title: String = DEFAULT_TXT,
    val path: String = DEFAULT_TXT,
    val uri: String = DEFAULT_TXT,
    val isRecorded: Boolean = DEFAULT_BOOLEAN,
    val size: Long = DEFAULT_LONG,
    val duration: Long = DEFAULT_LONG,
    val amplitude: String = DEFAULT_TXT
) {
    val nameWithoutFormat: String = title.substringBeforeLast(".")
}