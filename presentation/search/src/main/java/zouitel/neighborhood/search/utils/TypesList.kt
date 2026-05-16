package zouitel.neighborhood.search.utils

import zouitel.neighborhood.ui.CommonIcons

val typesList = listOf(
    Type(SearchTypes.Links, "Links", CommonIcons.LINK),
    Type(SearchTypes.Reminders, "Reminders", CommonIcons.BELL_RING),
    Type(SearchTypes.Audios, "Audios", CommonIcons.WAVEFORM),
    Type(SearchTypes.Medias, "Medias", CommonIcons.GALLERY),
    Type(SearchTypes.Properties(0), "Priorities", CommonIcons.CIRCLE_18)
)

data class Type(
    val types: SearchTypes,
    val title: String,
    val icon: Int
) {
    fun <T> isAvailable(list: List<T>) = list.isNotEmpty()
}