package zouitel.neighborhood.logic.cache

import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal fun ensureParentDir(file: File) {
    val parent = file.parentFile
    if (parent != null && !parent.exists()) {
        if (!parent.mkdirs() && !parent.exists()) {
            throw IOException("Failed to create directory: $parent")
        }
    }
}

/**
 * Replaces [file] using Android's backup-and-restore atomic file protocol.
 *
 * The previous contents remain recoverable until the new contents have been
 * fully written and committed. In particular, never delete the destination
 * as a fallback when a rename fails.
 */
internal fun atomicWrite(file: File, content: String) {
    ensureParentDir(file)
    val atomicFile = AtomicFile(file)
    var output: FileOutputStream? = null

    try {
        val stream = atomicFile.startWrite()
        output = stream
        stream.write(content.toByteArray(Charsets.UTF_8))
        atomicFile.finishWrite(stream)
        output = null
    } catch (failure: Throwable) {
        output?.let(atomicFile::failWrite)
        throw failure
    }
}
