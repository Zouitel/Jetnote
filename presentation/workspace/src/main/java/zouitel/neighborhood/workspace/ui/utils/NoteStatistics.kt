package zouitel.neighborhood.workspace.ui.utils

data class NoteStatistics(
    val wordCount: Int,
    val readingTimeMinutes: Int
)

private val noteWordPattern = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)*""")

fun calculateNoteStatistics(text: String): NoteStatistics {
    val wordCount = noteWordPattern.findAll(text).count()
    val readingTimeMinutes = if (wordCount == 0) 0 else (wordCount + 199) / 200
    return NoteStatistics(
        wordCount = wordCount,
        readingTimeMinutes = readingTimeMinutes
    )
}
