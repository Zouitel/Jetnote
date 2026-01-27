package zouitel.neighborhood.audio.state

import zouitel.neighborhood.audio.model.Audio

data class ItemUiState(
    val displayName: String,
    val size: String,
    val onClick: () -> Audio
)