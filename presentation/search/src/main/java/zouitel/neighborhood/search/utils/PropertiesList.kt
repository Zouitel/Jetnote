package zouitel.neighborhood.search.utils

import zouitel.neighborhood.workspace.model.Note

data class Property(
    val index: Int,
    val name: String,
    val color: Long
) {
    internal fun isAvailable(notes: List<Note>): Boolean {
        return notes.any { it.data.priority == index }
    }
}

val prioritiesList = listOf(
    Property(0,"Non", 0xffc0c0c0),
    Property(1,"Low", 0xff32cd32),
    Property(2,"Medium", 0xffffef00),
    Property(3,"High", 0xffffa500),
    Property(4,"Urgent", 0xffff2800),
)