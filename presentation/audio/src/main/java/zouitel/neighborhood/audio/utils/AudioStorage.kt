package zouitel.neighborhood.audio.utils

import java.io.File

/**
 * Keeps imported MediaStore items in a deterministic, collision-resistant app-private filename.
 * The source display name is retained (including its extension) for a useful file type hint.
 */
internal object AudioStorage {
    fun fileName(audioId: Long, title: String): String {
        val extension = title.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotBlank() && it.length <= 12 }
            ?.let { ".$it" }
            .orEmpty()
        return "audio_$audioId$extension"
    }

    fun destination(directory: File, audioId: Long, title: String): File =
        File(directory, fileName(audioId, title))
}
