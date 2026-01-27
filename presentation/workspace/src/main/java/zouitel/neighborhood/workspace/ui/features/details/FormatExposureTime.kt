package zouitel.neighborhood.workspace.ui.features.details

import java.util.Locale

// Helper function to format exposure time in a "fractal-like" pattern (e.g., 1/100 -> 1 / 100)
// or handle decimal exposure times.
fun formatExposureTime(exposureTime: String?): String {
    if (exposureTime.isNullOrEmpty()) return "N/A"
    return try {
        if (exposureTime.contains("/")) {
            val parts = exposureTime.split('/')
            if (parts.size == 2) {
                "${parts[0].trim()}/${parts[1].trim()} s"
            } else {
                exposureTime + "s"
            }
        } else {
            String.Companion.format(Locale.US, "%.3f s", exposureTime.toDouble())
        }
    } catch (_: NumberFormatException) {
        exposureTime + "s"
    }
}