package zouitel.neighborhood.workspace.ui.features.details

import java.util.Locale

data class DocumentProperties(
    val fileName        : String?  = null,  // File name without extension
    val extension       : String?  = null,  // e.g. "pdf", "docx", "xlsx"
    val mimeType        : String?  = null,  // e.g. "application/pdf"
    val sizeBytes       : Long?    = null,  // Raw byte count
    val lastModified    : Long?    = null,  // File-system last-modified epoch ms
    val pageCount       : Int?     = null,  // Number of pages (MuPDF)
    val metaTitle       : String?  = null,  // PDF /Title metadata
    val metaAuthor      : String?  = null,  // PDF /Author metadata
    val metaSubject     : String?  = null,  // PDF /Subject metadata
    val metaKeywords    : String?  = null,  // PDF /Keywords metadata
    val metaCreator     : String?  = null,  // Application that created the file
    val metaProducer    : String?  = null,  // PDF producer (e.g. "Adobe Acrobat")
    val metaCreationDate: String?  = null,  // PDF /CreationDate raw string
    val metaModDate     : String?  = null,  // PDF /ModDate raw string
    val metaFormat      : String?  = null,  // e.g. "PDF 1.7"
    val metaEncryption  : String?  = null,  // Encryption descriptor, if any
    val error           : String?  = null
) {
    // ── Derived helpers ───────────────────────────────────────────────────────

    val isValidInfo: Boolean
        get() = sizeBytes != null || pageCount != null || mimeType != null

    // ── Property map — same pattern as ImageProperties.properties() ───────────
    // Only non-null / non-blank values are included; the sheet renders them as chips.

    fun properties(): Map<String, String?> = buildMap {
        extension?.uppercase(Locale.getDefault())?.let       { put("Type",        it) }
        mimeType?.let                                         { put("MIME",        it) }
        sizeBytes?.let                                        { put("Size",        formatBytes(it)) }
        pageCount?.let                                        { put("Pages",       it.toString()) }
        metaTitle?.takeIf   { it.isNotBlank() }?.let         { put("Title",       it) }
        metaAuthor?.takeIf  { it.isNotBlank() }?.let         { put("Author",      it) }
        metaSubject?.takeIf { it.isNotBlank() }?.let         { put("Subject",     it) }
        metaKeywords?.takeIf{ it.isNotBlank() }?.let         { put("Keywords",    it) }
        metaCreator?.takeIf { it.isNotBlank() }?.let         { put("Creator",     it) }
        metaProducer?.takeIf{ it.isNotBlank() }?.let         { put("Producer",    it) }
        metaFormat?.takeIf  { it.isNotBlank() }?.let         { put("Format",      it) }
        metaEncryption?.takeIf { it.isNotBlank() && it != "None" }
                                                   ?.let    { put("Encryption",  it) }
        metaCreationDate?.takeIf { it.isNotBlank() }?.let   { put("Created",     formatPdfDate(it)) }
        metaModDate?.takeIf      { it.isNotBlank() }?.let   { put("Modified",    formatPdfDate(it)) }
    }.filterValues { !it.isNullOrBlank() }
}

/**
 * Parses a PDF date string (D:YYYYMMDDHHmmSS) into a readable form.
 * Falls back to the raw string if parsing fails.
 */
private fun formatPdfDate(raw: String): String {
    // PDF date format: D:YYYYMMDDHHmmSSOHH'mm'
    val cleaned = raw.removePrefix("D:").take(14) // keep YYYYMMDDHHmmSS
    return runCatching {
        val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val date = sdf.parse(cleaned)
        java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date!!)
    }.getOrElse { raw }
}

/**
 * Formats an exposure-time rational string like "1/100" → "1/100 s".
 * Reused by [ImageProperties] (declared here so it's available from the same package).
 */
internal fun formatExposureTime(et: String): String = if (et.contains('/')) "$et s" else "${et}s"