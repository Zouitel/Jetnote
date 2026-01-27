package zouitel.neighborhood.audio.state

import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.ui.CommonIcons

data class UiState(
    var isPlaying: Boolean = false,
    val icon: Int = CommonIcons.PLAY,
    val displayName: String = "",
    val progress: Float = 0F,
    val amplitudes: List<Float> = (0..100).toList().map { 1f },
    val playingAudio: Audio? = null,
    val isLoading: Boolean = false,
    val currentPath: String = ""
)