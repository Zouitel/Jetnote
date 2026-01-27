package zouitel.neighborhood.workspace.ui.features.details

import android.annotation.SuppressLint

data class VideoProperties(
    val exifDateTimeOriginal: String? = null, // Original date/time taken (often "YYYY:MM:DD HH:MM:SS")
    val sizeBytes: Long? = 1L,
    val mimeType: String? = null, // Mime type (e.g., "video/mp4")
    val format: String? = null, // Video format/container (e.g., "mp4", "mov")
    val resolution: String? = null, // Video resolution (e.g., "1920x1080")
    val bitrate: Long? = 1L, // Video bitrate in bits per second (e.g., 5000000 for 5 Mbps)
    val frameRate: Double? = 1.0, // Video frame rate (e.g., 29.97)
    val durationMillis: Long? = 1L, // Video duration in milliseconds
    val gpsCoordinates: String? = null,
    val lensInfo: String? = null, // Lens information (e.g., "iPhone 13 Pro Max back triple camera 6mm f/1.5")
    val exposureTime: String? = null, // Exposure time information (e.g., "1/100")
    val author: String? = null, // Author information (e.g., "John Doe")
    val description: String? = null, // Description information (e.g., "Landscape shot")
    val rotation: Int? = 1, // Rotation information (e.g., 90 for landscape)
    val error: String? = null
) {
    val isPartialInfo: Boolean
        get() = sizeBytes != null || exifDateTimeOriginal != null


    fun properties(): Map<String, String?> {
        return mapOf(
            "Format" to videoFormat(format),
            "Duration" to formatDuration(durationMillis),
            "Size Bytes" to formatBytes(sizeBytes ?: 0L),
            "Mime Type" to mimeType,
            "Resolution" to resolution,
            "Bitrate" to formatBitrate(bitrate),
            "Frame Rate" to formatFrameRate(frameRate),
            "GPS Coordinates" to gpsCoordinates,
            "Lens Info" to lensInfo,
            "Exposure Time" to formatExposureTime(exposureTime),
            "Author" to author,
            "Description" to description,
            "Rotation" to formatRotation(rotation),
        ).filterValues { it?.isNotEmpty() == true }
    }

    @SuppressLint("DefaultLocale")
    private fun formatDuration(durationMillis: Long?): String? {
        return durationMillis?.let {
            val seconds = (it / 1000) % 60
            val minutes = (it / (1000 * 60)) % 60
            val hours = (it / (1000 * 60 * 60)) % 24

            if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatBitrate(bitrate: Long?): String? {
        return bitrate?.let {
            val kbps = it / 1000.0
            val mbps = kbps / 1000.0
            when {
                mbps >= 1 -> String.format("%.2f Mbps", mbps)
                kbps >= 1 -> String.format("%.2f Kbps", kbps)
                else -> String.format("%d bps", it)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun videoFormat(format: String?): String? {
        return format?.let {
            // Example: "video/mp4" -> "MP4", "video/x-matroska" -> "MKV"
            val simpleFormat = it.substringAfterLast('/').uppercase()
            when (simpleFormat) {
                "X-MATROSKA" -> "MKV"
                "QUICKTIME" -> "MOV"
                "X-FLV" -> "FLV"
                "X-MSVIDEO" -> "AVI"
                "X-MS-WMV" -> "WMV"
                "X-MS-ASF" -> "ASF"
                else -> simpleFormat
            }
        }
    }

    private fun formatFrameRate(rate: Double?): String? {
        return rate?.let { "%.2f fps".format(it) }
    }

    private fun formatRotation(rotation: Int?): String? {
        return rotation?.let { "$it°" }
    }
}

