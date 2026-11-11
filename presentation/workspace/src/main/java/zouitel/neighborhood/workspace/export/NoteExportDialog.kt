package zouitel.neighborhood.workspace.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Material3
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.workspace.model.Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Save-one-note dialog. Call this from the current note's overflow menu or toolbar.
 * The Storage Access Framework owns the destination and no broad storage permission is needed.
 */
@Composable
fun NoteExportDialog(
    note: Data,
    onDismiss: () -> Unit,
    onExported: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<NoteExporter.ExportDocument?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val document = pending
        if (uri != null && document != null) {
            scope.launch {
                busy = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(document.bytes) }
                            ?: error("Unable to open the selected destination.")
                    }
                }.onSuccess {
                    onExported(document.fileName)
                    onDismiss()
                }.onFailure {
                    error = it.message ?: "Export failed."
                }
                busy = false
                pending = null
            }
        } else {
            pending = null
        }
    }

    fun save(document: NoteExporter.ExportDocument) {
        pending = document
        saveLauncher.launch(document.fileName)
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Export note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a format for “${note.title.ifBlank { "Untitled note" }}”.")
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Text("Preparing export…", modifier = Modifier.padding(start = 12.dp))
                    }
                } else {
                    LazyColumn {
                        items(NoteExporter.Format.entries.toList()) { format ->
                            val label = when (format) {
                                NoteExporter.Format.EXCEL -> "Excel (.xlsx)"
                                NoteExporter.Format.MARKDOWN -> "Markdown (.md)"
                                NoteExporter.Format.AUDIO -> "Audio narration (.wav)"
                                NoteExporter.Format.TEXT -> "Plain text (.txt)"
                                NoteExporter.Format.PDF -> "PDF (.pdf)"
                                NoteExporter.Format.WORD -> "Word (.docx)"
                                NoteExporter.Format.SUBTITLES -> "Subtitles (.srt)"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !busy) {
                                        error = null
                                        if (format == NoteExporter.Format.AUDIO) {
                                            busy = true
                                            scope.launch {
                                                runCatching {
                                                    withContext(Dispatchers.IO) {
                                                        NoteExporter.audio(context, note)
                                                    }
                                                }.onSuccess(::save)
                                                    .onFailure { error = it.message ?: "Audio export failed." }
                                                busy = false
                                            }
                                        } else {
                                            runCatching {
                                                when (format) {
                                                    NoteExporter.Format.EXCEL -> NoteExporter.excel(note)
                                                    NoteExporter.Format.MARKDOWN -> NoteExporter.markdownDocument(note)
                                                    NoteExporter.Format.TEXT -> NoteExporter.textDocument(note)
                                                    NoteExporter.Format.PDF -> NoteExporter.pdf(note)
                                                    NoteExporter.Format.WORD -> NoteExporter.word(note)
                                                    NoteExporter.Format.SUBTITLES -> NoteExporter.subtitles(note)
                                                    NoteExporter.Format.AUDIO -> error("Audio export unavailable.")
                                                }
                                            }.onSuccess(::save)
                                                .onFailure { error = it.message ?: "Export failed." }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                error?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
        }
    )
}
