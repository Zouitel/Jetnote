package zouitel.neighborhood.audio.utils

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Provides one app-private location and deterministic, collision-resistant filenames for imports.
 */
internal object AudioStorage {
    fun directory(context: Context): File =
        context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
            ?: File(context.filesDir, Environment.DIRECTORY_PODCASTS)

    fun fileName(audioId: Long, title: String): String {
        val extension = title.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.matches(Regex("[A-Za-z0-9]{1,10}")) }
            ?.let { ".$it" }
            .orEmpty()
        return "audio_$audioId$extension"
    }

    fun destination(directory: File, audioId: Long, title: String): File =
        File(directory, fileName(audioId, title))
}
