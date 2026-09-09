package zouitel.neighborhood.workspace.ui.features.details

import androidx.exifinterface.media.ExifInterface
import java.util.Locale

data class ImageProperties(
    val exifDateTimeOriginal: String? = null, // Original date/time taken (often "YYYY:MM:DD HH:MM:SS")
    val exifMake: String? = null,          // Camera manufacturer
    val exifModel: String? = null,         // Camera model
    val mediaTitle: String? = null, // Media title, often from MediaStore.MediaColumns.TITLE
    val mimeType: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long? = null,
    val lastModified: Long? = null, // File system last modified date (from ContentResolver)
    val exifExposureTime: String? = null, // Shutter speed
    val exifFNumber: String? = null,       // Aperture (f-number)
    val exifIsoSpeedRatings: String? = null, // ISO speed
    val exifFocalLength: String? = null,   // Focal length (in mm)
    val exifGpsLatitude: Double? = null,   // GPS Latitude
    val exifGpsLongitude: Double? = null,  // GPS Longitude
    val exifExposureCompensation: String? = null, // Exposure compensation (e.g., "+1.0", "-0.5")
    val exifMegapixels: Double? = null,        // Calculated megapixels (width * height / 1,000,000)
    val exifOrientation: Int? = null,      // Image orientation (e.g., 1=Normal, 6=Rotate 90)
    val exifLensModel: String? = null,     // Lens model
    val exifMeteringMode: Int? = null,  // Metering mode (e.g., Average, CenterWeightedAverage, Spot)
    val exifFlash: Int? = null,            // Flash mode (e.g., 0=No Flash, 1=Fired, 5=Fired, Return not detected, etc.)
    val exifExposureProgram: Int? = null, // Exposure program (e.g., 0=Not defined, 1=Manual, 2=Normal program)
    val exifWhiteBalance: Int? = null,   // White balance (e.g., 0=Auto, 1=Manual)
    val exifSoftware: String? = null,      // Software used to process the image
    val exifCopyright: String? = null,     // Copyright information
    val exifImageDescription: String? = null, // Image description or user comment
    val error: String? = null
) {
    val isValidDimensions: Boolean
        get() = width > 0 && height > 0

    val isPartialInfo: Boolean
        get() = sizeBytes != null || lastModified != null || exifDateTimeOriginal != null || exifMake != null // etc.

    val dimensions: String
        get() = "${width}x$height"

    private fun formatMimeType(mimeType: String?): String? {
        val type = mimeType?.substringAfterLast('/')?.uppercase(Locale.getDefault())
        return when (type) {
            "JPEG", "JPG" -> "JPEG"
            "PNG" -> "PNG"
            "GIF" -> "GIF"
            else -> type // Keep original for other types or if no slash
        }
    }

    fun properties(): Map<String, String?> {
        return mapOf(
//            "Title" to mediaTitle,
            "MIME Type" to formatMimeType(mimeType),
            "Dimensions" to dimensions,
            "Size" to (sizeBytes?.let { formatBytes(it) }),
            "Exposure Time" to (exifExposureTime?.let { formatExposureTime(it) }),
            "F-Number" to (exifFNumber?.let { formatFNumber(it) }),
            "ISO Speed" to (exifIsoSpeedRatings?.let { "ISO $it" }),
            "Focal Length" to (exifFocalLength?.let { formatFocalLength(it) }),
            "GPS Latitude" to (exifGpsLatitude?.let { formatGpsCoordinate(it, isLatitude = true) }),
            "GPS Longitude" to (exifGpsLongitude?.let { formatGpsCoordinate(it, isLatitude = false) }),
            "Exposure Compensation" to (exifExposureCompensation?.let { parseRationalToString(it) }),
            "Megapixels" to (exifMegapixels?.let {
                String.format(Locale.getDefault(), "%.2fMP", (width * height) / 1_000_000.0)
            }),
            "Orientation" to (exifOrientation?.let { mapOrientationToString(it) }),
            "Lens Model" to (exifLensModel),
            "Metering Mode" to (exifMeteringMode?.let { mapMeteringModeToString(it) }),
            "Flash" to (exifFlash?.let { mapFlashModeToString(it) }),
            "Exposure Program" to (exifExposureProgram?.let { mapExposureProgramToString(it) }),
            "White Balance" to (exifWhiteBalance?.let { mapWhiteBalanceToString(it) }),
            "Software" to (exifSoftware),
            "Copyright" to (exifCopyright),
            "Description" to (exifImageDescription)
        ).filterValues { it.isNullOrEmpty().not() }
    }



    // Helper function to format f-number (aperture)
    private fun formatFNumber(fNumber: String?): String {
        if (fNumber.isNullOrEmpty()) return "N/A"
        return try {
            String.Companion.format(Locale.US, "F%.1f", fNumber.toDouble())
        } catch (_: NumberFormatException) {
            "F$fNumber"
        }
    }

    // Helper function to format focal length (which is often a rational number string like "50/1")
    private fun formatFocalLength(focalLength: String?): String {
        if (focalLength.isNullOrEmpty()) return "N/A"
        return try {
            if (focalLength.contains("/")) {
                val parts = focalLength.split('/')
                if (parts.size == 2) {
                    val numerator = parts[0].toDouble()
                    val denominator = parts[1].toDouble()
                    if (denominator != 0.0) String.Companion.format(Locale.US, "%.1f mm", numerator / denominator) else "$focalLength mm"
                } else focalLength + "mm"
            } else focalLength + "mm"
        } catch (_: NumberFormatException) {
            focalLength + "mm"
        }
    }

    // Helper function to format GPS coordinates
    private fun formatGpsCoordinate(coordinate: Double, isLatitude: Boolean): String {
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        val absCoord = Math.abs(coordinate)
        val degrees = absCoord.toInt()
        val minutesDouble = (absCoord - degrees) * 60
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60
        return String.Companion.format(
            Locale.US,
            "%d° %d' %.2f\" %s",
            degrees, minutes, seconds, direction
        )
    }
    // Helper to map Metering Mode int to String
    private fun mapMeteringModeToString(mode: Int): String {
        return when (mode) {
            0 -> "Unknown"
            1 -> "Average"
            2 -> "CenterWeightedAverage"
            3 -> "Spot"
            4 -> "MultiSpot"
            5 -> "Pattern"
            6 -> "Partial"
            255 -> "Other"
            else -> "Reserved ($mode)"
        }
    }

    // Helper to map Flash Mode int to String
    private fun mapFlashModeToString(mode: Int): String {
        return when (mode) {
            0x0 -> "No Flash"
            0x1 -> "Fired"
            0x5 -> "Fired, Return not detected"
            0x7 -> "Fired, Return detected"
            0x8 -> "On, Did not fire"
            0x9 -> "On, Fired"
            0xD -> "On, Return not detected"
            0xF -> "On, Return detected"
            0x10 -> "Off, Did not fire"
            0x18 -> "Off, Did not fire, Return not detected"
            0x19 -> "Auto, Did not fire"
            0x1D -> "Auto, Fired, Return not detected"
            0x1F -> "Auto, Fired, Return detected"
            0x20 -> "No flash function"
            0x30 -> "Off, No flash function"
            0x41 -> "Fired, Red-eye reduction"
            0x45 -> "Fired, Red-eye reduction, Return not detected"
            0x47 -> "Fired, Red-eye reduction, Return detected"
            0x49 -> "On, Red-eye reduction"
            0x4D -> "On, Red-eye reduction, Return not detected"
            0x4F -> "On, Red-eye reduction, Return detected"
            0x59 -> "Auto, Did not fire, Red-eye reduction"
            0x5D -> "Auto, Fired, Red-eye reduction, Return not detected"
            0x5F -> "Auto, Fired, Red-eye reduction, Return detected"
            else -> "Unknown ($mode)"
        }
    }

    private fun mapWhiteBalanceToString(mode: Int): String {
        return when (mode) {
            ExifInterface.WHITEBALANCE_AUTO -> "Auto"
            ExifInterface.WHITEBALANCE_MANUAL -> "Manual"
            else -> "Unknown ($mode)"
        }
    }

    private fun mapExposureProgramToString(program: Int): String {
        return when (program) {
            0 -> "Not defined"
            1 -> "Manual"
            2 -> "Normal program"
            3 -> "Aperture priority"
            4 -> "Shutter priority"
            5 -> "Creative program (biased toward depth of field)"
            6 -> "Action program (biased toward fast shutter speed)"
            7 -> "Portrait mode (for closeup photos with the background out of focus)"
            8 -> "Landscape mode (for landscape photos with the background in focus)"
            else -> "Unknown ($program)"
        }
    }

    // Helper function to map orientation value to a human-readable string
    private fun mapOrientationToString(orientation: Int?): String {
        return when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> "Normal"
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flip horizontal"
            ExifInterface.ORIENTATION_ROTATE_180 -> "Rotate 180"
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flip vertical"
            ExifInterface.ORIENTATION_TRANSPOSE -> "Transpose"
            ExifInterface.ORIENTATION_ROTATE_90 -> "Rotate 90"
            ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse"
            ExifInterface.ORIENTATION_ROTATE_270 -> "Rotate 270"
            ExifInterface.ORIENTATION_UNDEFINED -> "Undefined"
            else -> "Unknown ($orientation)"
        }
    }

    private fun parseRationalToString(rationalString: String?): String? {
        if (rationalString == null) return null
        return try {
            val parts = rationalString.split('/')
            if (parts.size == 2) {
                val numerator = parts[0].toDouble()
                val denominator = parts[1].toDouble()
                if (denominator != 0.0) String.Companion.format(
                    Locale.US, "%.1f" + "ev", numerator / denominator
                ) else rationalString
            } else rationalString
        } catch (_: NumberFormatException) {
            rationalString
        }
    }
}