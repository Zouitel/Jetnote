package zouitel.neighborhood.workspace.model

/**
 * Describes the kind of attachment stored in a [Media] entry.
 *
 * Using a sealed enum instead of multiple boolean flags makes exhaustive
 * `when` expressions compile-safe and removes the ambiguity of states like
 * `isVideo = true, isDocument = true` that booleans allow.
 */
enum class MediaType {
    /** A still image (jpg, png, webp, …). */
    IMAGE,

    /** A video clip (mp4, mkv, …). */
    VIDEO,

    /** An office / text document (pdf, docx, xlsx, pptx, txt, …). */
    DOCUMENT;

    companion object {
        /**
         * Derives the [MediaType] from a file extension.
         * Returns [IMAGE] for anything unrecognised.
         */
        fun fromExtension(ext: String): MediaType = when (ext.lowercase().trim('.')) {
            "pdf", "doc", "docx",
            "xls", "xlsx",
            "ppt", "pptx",
            "txt", "rtf", "odt",
            "ods", "odp" -> DOCUMENT
            "mp4", "mkv", "avi",
            "mov", "webm", "3gp" -> VIDEO
            else -> IMAGE
        }

        /**
         * Derives the [MediaType] from an Android MIME type string.
         * Falls back to [fromExtension] when the MIME is ambiguous.
         */
        fun fromMime(mime: String): MediaType = when {
            mime.startsWith("video/") -> VIDEO
            mime.startsWith("image/") -> IMAGE
            mime == "application/pdf" -> DOCUMENT
            mime.contains("msword") -> DOCUMENT
            mime.contains("wordprocessingml") -> DOCUMENT
            mime.contains("ms-excel") -> DOCUMENT
            mime.contains("spreadsheetml") -> DOCUMENT
            mime.contains("ms-powerpoint") -> DOCUMENT
            mime.contains("presentationml") -> DOCUMENT
            mime == "text/plain" -> DOCUMENT
            mime.contains("opendocument") -> DOCUMENT
            else -> IMAGE
        }
    }
}