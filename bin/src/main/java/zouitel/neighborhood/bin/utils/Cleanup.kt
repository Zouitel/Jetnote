package zouitel.neighborhood.bin.utils


import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

suspend fun cleanupOrphanedAudios(
    context: Context,
    databaseAudioPaths: List<String>,
    ioDispatcher: CoroutineDispatcher
): Int = withContext(ioDispatcher) {
    var deletedCount = 0

    runCatching {
        // Get Music directory using Context API
        val musicDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) ?: return@withContext 0


        if (!musicDir.exists()) {
            return@withContext 0
        }

        // Convert database paths to a Set for O(1) lookup
        val databasePathsSet = databaseAudioPaths
            .map { normalizePath(it) }
            .toSet()

        // Get all audio files from Music directory
        val audioFiles = musicDir.walkTopDown()
            .filter { it.isFile && isAudioFile(it) }
            .toList()
        // Delete files that don't exist in database
        audioFiles.forEach { file ->
            val normalizedPath = normalizePath(file.absolutePath)

            if (normalizedPath !in databasePathsSet) {
                runCatching {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
        }
    }.onFailure {
        it.printStackTrace()
    }

    return@withContext deletedCount
}

/**
 * Check if a file is an audio based on extension
 */
private fun isAudioFile(file: File): Boolean {
    val audioExtensions = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma",
        "opus", "amr", "3gp", "mid", "midi", "ape", "alac"
    )
    val extension = file.extension.lowercase()
    return extension in audioExtensions
}

suspend fun cleanupOrphanedImages(
    context: Context,
    imagePaths: List<String>,
    ioDispatcher: CoroutineDispatcher
): Int = withContext(ioDispatcher) {
    var deletedCount = 0

    runCatching {
        // Get Pictures directory using Context API
        val picturesDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return@withContext 0

        if (!picturesDir.exists()) {
            return@withContext 0
        }

        // Convert database paths to a Set for O(1) lookup
        val databasePathsSet = imagePaths
            .map { normalizePath(it) }
            .toSet()

        // Get all image files from Pictures directory
        val imageFiles = picturesDir.walkTopDown()
            .filter { it.isFile && isImageFile(it) }
            .toList()

        // Delete files that don't exist in database
        imageFiles.forEach { file ->
            val normalizedPath = normalizePath(file.absolutePath)

            if (normalizedPath !in databasePathsSet) {
                runCatching {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
        }
    }.onFailure {
        it.printStackTrace()
    }
    return@withContext deletedCount
}

/**
 * Check if a file is an image based on extension
 */
private fun isImageFile(file: File): Boolean {
    val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
    val extension = file.extension.lowercase()
    return extension in imageExtensions
}

suspend fun cleanupOrphanedVideos(
    context: Context,
    videoPaths: List<String>,
    ioDispatcher: CoroutineDispatcher
): Int = withContext(ioDispatcher) {
    var deletedCount = 0

    runCatching {
        // Get Movies directory using Context API
        val moviesDir =
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return@withContext 0

        if (!moviesDir.exists()) {
            return@withContext 0
        }

        // Convert database paths to a Set for O(1) lookup
        val databasePathsSet = videoPaths
            .map { normalizePath(it) }
            .toSet()

        // Get all video files from Movies directory
        val videoFiles = moviesDir.walkTopDown()
            .filter { it.isFile && isVideoFile(it) }
            .toList()

        // Delete files that don't exist in database
        videoFiles.forEach { file ->
            val normalizedPath = normalizePath(file.absolutePath)

            if (normalizedPath !in databasePathsSet) {
                runCatching {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
        }
    }.onFailure {
        it.printStackTrace()
    }

    return@withContext deletedCount
}

/**
 * Check if a file is a video based on extension
 */
private fun isVideoFile(file: File): Boolean {
    val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
        "m4v", "3gp", "3g2", "mpeg", "mpg", "ts", "mts"
    )
    val extension = file.extension.lowercase()
    return extension in videoExtensions
}

/**
 * Normalize path for consistent comparison
 */
private fun normalizePath(path: String): String {
    return path.trim().replace("\\", "/")
}
