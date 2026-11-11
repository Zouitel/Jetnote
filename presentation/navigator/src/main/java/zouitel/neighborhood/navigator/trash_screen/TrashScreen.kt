package zouitel.neighborhood.navigator.trash_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class TrashScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<TrashScreenModel>()
        val notes by model.notes.collectAsState()
        var showEmptyConfirmation by remember { mutableStateOf(false) }
        var permanentlyDeleteUid by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) { model.observe() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Trash") },
                    actions = {
                        TextButton(
                            enabled = notes.isNotEmpty(),
                            onClick = { showEmptyConfirmation = true }
                        ) { Text("Empty") }
                    }
                )
            }
        ) { padding ->
            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Trash is empty", style = MaterialTheme.typography.titleLarge)
                    Text("Deleted notes stay here for 30 days before they're permanently removed.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.data.uid }) { note ->
                        val deletedAt = note.data.deletedAt ?: 0L
                        val elapsed = (System.currentTimeMillis() - deletedAt).coerceAtLeast(0L)
                        val remaining = (30L - TimeUnit.MILLISECONDS.toDays(elapsed)).coerceAtLeast(0L)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(note.data.title.ifBlank { "Untitled note" }, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (remaining == 0L) "Expires today" else "$remaining days left",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { model.restore(note.data.uid) }) {
                                        Text("Restore")
                                    }
                                    TextButton(onClick = { permanentlyDeleteUid = note.data.uid }) {
                                        Text("Delete permanently")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEmptyConfirmation) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirmation = false },
                title = { Text("Empty Trash?") },
                text = { Text("All notes in Trash will be permanently deleted. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        model.empty()
                        showEmptyConfirmation = false
                    }) { Text("Delete all") }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyConfirmation = false }) { Text("Cancel") }
                }
            )
        }

        permanentlyDeleteUid?.let { uid ->
            AlertDialog(
                onDismissRequest = { permanentlyDeleteUid = null },
                title = { Text("Delete permanently?") },
                text = { Text("This note cannot be restored after deletion.") },
                confirmButton = {
                    TextButton(onClick = {
                        model.deletePermanently(uid)
                        permanentlyDeleteUid = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { permanentlyDeleteUid = null }) { Text("Cancel") }
                }
            )
        }
    }
}
