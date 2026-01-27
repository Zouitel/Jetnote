package zouitel.neighborhood.workspace.ui.features.details

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

// Helper function to format byte size for display (same as before)
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Bytes"
    val units = arrayOf("Bytes", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(), "%.2f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups])
}