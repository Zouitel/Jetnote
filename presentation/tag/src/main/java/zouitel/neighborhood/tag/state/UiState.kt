package zouitel.neighborhood.tag.state

data class UiState(
    val currentId: Long = 0L,
    val currentColor: Int? = null,
    val isColorsDialog: Boolean = false,
    val currentLabel: String = ""
)
