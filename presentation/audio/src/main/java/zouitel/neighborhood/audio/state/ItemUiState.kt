package zouitel.neighborhood.audio.state

import androidx.compose.runtime.Stable
import zouitel.neighborhood.audio.model.Audio

@Stable
data class ItemUiState(
    val displayName: String,
    val size: String,
    val onClick: () -> Audio
)