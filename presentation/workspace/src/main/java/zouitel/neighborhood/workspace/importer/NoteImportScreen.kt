package zouitel.neighborhood.workspace.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import java.util.UUID

class NoteImportScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val dataModel = koinScreenModel<DataScreenModel>()
        val scope = rememberCoroutineScope()
        val imported = remember { mutableStateListOf<NoteImportParser.ParsedNote>() }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        val picker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isNotEmpty()) {
                busy = true
                error = null
                scope.launch {
                    val results = withContext(Dispatchers.IO) {
                        uris.map { uri -> runCatching { NoteImportParser.parse(context, uri) } }
                    }
                    imported.clear()
                    imported.addAll(results.mapNotNull { it.getOrNull() })
                    error = results.firstNotNullOfOrNull { it.exceptionOrNull()?.message }
                    busy = false
                    if (imported.isEmpty() && error == null) error = "No supported files were selected."
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Import notes") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(painterResource(CommonIcons.ARROW_LEFT), contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Bring your notes into Jetnote. Select one or more supported files; each file becomes a separate note.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { picker.launch(arrayOf(
                        "text/plain",
                        "text/markdown",
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/x-subrip",
                        "application/octet-stream"
                    )) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imported.isEmpty()) "Choose files" else "Choose different files")
                }

                Text(
                    "Supported: TXT, Markdown, PDF, DOCX, XLSX and SRT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (busy) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Reading selected files…")
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                if (imported.isNotEmpty()) {
                    Text("${imported.size} file(s) ready to import", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(imported, key = { it.fileName }) { note ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(note.title, style = MaterialTheme.typography.titleSmall)
                                    Text(note.fileName, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        note.blocks.take(3).joinToString("\n") { block ->
                                            (block.content as? BlockContent.Text)?.text.orEmpty()
                                        }.take(320),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("${note.blocks.size} content blocks", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val userUid = FirebaseAuth.getInstance().currentUser?.uid
                            if (userUid.isNullOrBlank()) {
                                error = "Sign in before importing notes so they can be saved to your account."
                                return@Button
                            }
                            busy = true
                            error = null
                            scope.launch {
                                var created = 0
                                imported.toList().forEach { note ->
                                    runCatching {
                                        dataModel.persist(
                                            Data(
                                                uid = UUID.randomUUID().toString(),
                                                userUid = userUid,
                                                title = note.title,
                                                blocks = note.blocks
                                            )
                                        )
                                    }.onSuccess { created++ }
                                        .onFailure { error = it.message ?: "A note could not be saved." }
                                }
                                busy = false
                                if (created > 0) {
                                    imported.clear()
                                    navigator.pop()
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import ${imported.size} note(s)")
                    }
                } else if (!busy) {
                    Text(
                        "Your original files stay where they are. Jetnote reads their contents and creates new notes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
