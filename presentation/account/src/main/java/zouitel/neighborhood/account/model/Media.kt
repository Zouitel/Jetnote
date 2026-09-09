package zouitel.neighborhood.account.model

import android.graphics.Color
import androidx.annotation.Keep
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Keep
@Serializable
data class Media(
    val id: Long = Random.nextLong(),
    val uri: String = DEFAULT_TXT,
    val isVideo: Boolean = false,
    val title: String? = DEFAULT_TXT,
    val color: Int = Color.BLACK
)