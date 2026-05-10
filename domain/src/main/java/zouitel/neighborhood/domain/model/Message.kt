package zouitel.neighborhood.domain.model

import androidx.annotation.Keep

@Keep
data class Message(
    val id: Long,
    val isRequest: Boolean,
    val prompt: String
)
