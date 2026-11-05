package zouitel.neighborhood.recorder.state

data class UiState(
    val isRecording: Boolean = false,
    val isPause: Boolean = false,
    val isStarting: Boolean = false,
    val seconds: String = "00",
    val minutes: String = "00",
    val hours: String = "00",
    val elapsedMilliseconds: Long = 0L,
    val errorMessage: String? = null,
)
