package zouitel.neighborhood.workspace.model

/** Describes the kind of attachment stored in a [Media] entry. */
enum class MediaType {
    IMAGE, VIDEO, DOCUMENT;

    companion object {
        /** Derives a media type from an extension. Unknown extensions keep the legacy IMAGE fallback. */
        fun fromExtension(ext: String): MediaType = when (ext.substringAfterLast('.', ext).lowercase().trim()) {
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "rtf", "odt", "ods", "odp" -> DOCUMENT
            "mp4", "mkv", "avi", "mov", "webm", "3gp" -> VIDEO
            else -> IMAGE
        }

        /** Uses MIME metadata first, then a supplied extension for generic or unknown MIME types. */
        fun fromMime(mime: String?, extension: String? = null): MediaType {
            val value = mime?.substringBefore(';')?.trim()?.lowercase().orEmpty()
            return when {
                value.startsWith("video/") -> VIDEO
                value.startsWith("image/") -> IMAGE
                value == "application/pdf" || value.startsWith("text/") ||
                    value.contains("msword") || value.contains("wordprocessingml") ||
                    value.contains("ms-excel") || value.contains("spreadsheetml") ||
                    value.contains("ms-powerpoint") || value.contains("presentationml") ||
                    value.contains("opendocument") -> DOCUMENT
                else -> extension?.let(::fromExtension) ?: IMAGE
            }
        }
    }
}
