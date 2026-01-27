package zouitel.neighborhood.search.utils

sealed class SearchTypes {
    data class Text(val value: String): SearchTypes()
    object Links: SearchTypes()
    object Audios: SearchTypes()
    object Reminders: SearchTypes()
    object Tasks: SearchTypes()
    data class Tag(val label: String): SearchTypes()
    data class Image(val path: String): SearchTypes()
    data class Color(val value: Int?): SearchTypes()
    object Medias: SearchTypes()
    data class  Properties(val value: Int): SearchTypes()
}