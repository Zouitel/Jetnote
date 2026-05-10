package zouitel.neighborhood.workspace.ui.features.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifex.mupdf.fitz.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.SettingsScreenModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A [ModalBottomSheet] that displays metadata for a document file — name,
 * type, size, page count, and any embedded PDF metadata (author, producer …).
 *
 * Mirrors [ImageDetails] in structure and visual style:
 *  • Loading spinner while IO work runs
 *  • Error text in red if something goes wrong
 *  • Date + author line at the top in bold
 *  • Tappable chips (tap to reveal the key label) via [ContextualFlowRow]
 *
 * @param path     Absolute file-system path to the document.
 * @param onClose  Called when the sheet should be dismissed.
 */
@Composable
fun DocumentDetails(path: String?, onClose: () -> Unit) {
    if (path == null) return

    var docProperties by remember { mutableStateOf<DocumentProperties?>(null) }
    var isLoading     by remember { mutableStateOf(false) }

    val fileDoesNotExistResource = stringResource(R.string.file_does_not_exist)
    val couldNotReadPdfMetadataResource = stringResource(R.string.could_not_read_pdf_metadata)
    val securityErrorProcessingFileResource = stringResource(R.string.security_error_processing_file)
    val anUnexpectedErrorOccurredResource = stringResource(R.string.an_unexpected_error_occurred)
    // ── Load all metadata on IO ───────────────────────────────────────────────
    LaunchedEffect(path) {
        isLoading     = true
        docProperties = null

        val loaded = withContext(Dispatchers.IO) {
            var props = DocumentProperties()

            try {
                val file = File(path)

                if (!file.exists()) {
                    return@withContext DocumentProperties(
                        error = "$fileDoesNotExistResource: $path"
                    )
                }

                // ── 1. Basic file info ────────────────────────────────────────
                props = props.copy(
                    fileName     = file.nameWithoutExtension,
                    extension    = file.extension.lowercase(Locale.getDefault()),
                    sizeBytes    = file.length(),
                    lastModified = file.lastModified()
                )

                // ── 2. MIME type from extension ───────────────────────────────
                val mime = mimeFromExtension(file.extension.lowercase(Locale.getDefault()))
                props = props.copy(mimeType = mime)

                // ── 3. MuPDF metadata (works for PDF; partial for Office docs) ─
                runCatching {
                    val doc = Document.openDocument(path)

                    val pageCount = doc.countPages()

                    // MuPDF exposes standard PDF info-dict keys via getMetaData().
                    fun meta(key: String) = runCatching {
                        doc.getMetaData(key).takeIf { it.isNotBlank() }
                    }.getOrNull()

                    props = props.copy(
                        pageCount        = pageCount.takeIf { it > 0 },
                        metaTitle        = meta(Document.META_INFO_TITLE),
                        metaAuthor       = meta(Document.META_INFO_AUTHOR),
                        metaSubject      = meta(Document.META_INFO_SUBJECT),
                        metaKeywords     = meta(Document.META_INFO_KEYWORDS),
                        metaCreator      = meta(Document.META_INFO_CREATOR),
                        metaProducer     = meta(Document.META_INFO_PRODUCER),
                        metaCreationDate = meta(Document.META_INFO_CREATIONDATE),
//                        metaModDate      = meta(Document.META_INFO_MODDATE),
                        metaFormat       = meta(Document.META_FORMAT),
                        metaEncryption   = meta(Document.META_ENCRYPTION)
                    )

                    doc.destroy()
                }.onFailure { e ->
                    // MuPDF can't parse .docx/.xlsx/.pptx — that's expected.
                    // We still have the basic file info, so this is non-fatal.
                    props = props.copy(
                        error = if (mime?.contains("pdf") == true)
                            "$couldNotReadPdfMetadataResource: ${e.message}" else null
                    )
                }

            } catch (e: SecurityException) {
                props = props.copy(error = "$securityErrorProcessingFileResource: ${e.message}")
            } catch (e: Exception) {
                props = props.copy(error = "$anUnexpectedErrorOccurredResource: ${e.message}")
            }

            props
        }

        docProperties = loaded
        isLoading     = false
    }

    // ── Sheet UI — identical layout pattern to ImageDetails ──────────────────
    ModalBottomSheet(onClose) {
        Column(Modifier.padding(horizontal = 8.dp)) {

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(stringResource(R.string.loading_media_info))
                }

                docProperties?.error != null && !docProperties!!.isValidInfo -> {
                    // Fatal error — nothing useful to show
                    Text(stringResource(R.string.error), fontWeight = FontWeight.Bold, color = Color.Red)
                    Text(docProperties!!.error!!, color = Color.Red)
                }

                docProperties != null -> {
                    val props = docProperties!!

                    // ── Non-fatal error banner ────────────────────────────────
                    if (!props.error.isNullOrBlank()) {
                        Text("${stringResource(R.string.something_wrong_with_file)}Note:", fontWeight = FontWeight.Bold, color = Color.Red)
                        Text(props.error, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── Header: last-modified date ────────────────────────────
                    props.lastModified?.let { epoch ->
//                        runCatching {
                            val fmt = SimpleDateFormat(
                                "EEEE, dd MMMM yyyy, HH:mm", Locale.getDefault()
                            )
                            Text(fmt.format(epoch), fontWeight = FontWeight.Bold)
//                        }
                    }

                    // ── Subheader: file name + author ─────────────────────────
                    val subHeader = buildString {
                        props.fileName?.let { append(it) }
                        props.metaAuthor?.takeIf { it.isNotBlank() }?.let {
                            if (isNotEmpty()) append("  ·  ")
                            append(it)
                        }
                    }
                    if (subHeader.isNotBlank()) {
                        Text(subHeader, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Chip grid — tap to reveal property key ────────────────
                    val entries = props.properties().entries.toList()
                    if (entries.isNotEmpty()) {
                        ContextualFlowRow(
                            itemCount = entries.size
                        ) { index ->
                            var showKey by remember { mutableStateOf(false) }
                            val entry = entries[index]
                            AssistChip(
                                modifier = Modifier.padding(horizontal = 3.dp),
                                onClick  = { showKey = !showKey },
                                label    = {
                                    Text(
                                        text     = if (showKey) entry.key else entry.value ?: "",
                                        modifier = Modifier.animateContentSize()
                                    )
                                },
                                shape  = MaterialTheme.shapes.large,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (showKey) CommonColors.prime
                                                     else MaterialTheme.colorScheme.surface,
                                    labelColor     = Color.LightGray
                                ),
                                border = BorderStroke(1.dp, CommonColors.prime)
                            )
                        }
                    } else {
                        Text(stringResource(R.string.no_metadata_available_for_this_document))
                    }
                }
                else -> Text(stringResource(R.string.no_file_path_provided))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── MIME helper ───────────────────────────────────────────────────────────────

private fun mimeFromExtension(ext: String): String? = when (ext) {
    "pdf"  -> "application/pdf"
    "doc"  -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "xls"  -> "application/vnd.ms-excel"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "ppt"  -> "application/vnd.ms-powerpoint"
    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "txt"  -> "text/plain"
    else   -> null
}