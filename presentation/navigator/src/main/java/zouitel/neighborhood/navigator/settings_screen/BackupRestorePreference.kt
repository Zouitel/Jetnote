package zouitel.neighborhood.navigator.settings_screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import zouitel.neighborhood.database.backup.BackupArchiveManager

@Composable
internal fun BackupRestorePreference(
    onMessage: (String) -> Unit
) {
    val backupManager = koinInject<BackupArchiveManager>()
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching { backupManager.createBackup(uri) }
                .onSuccess { onMessage("Backup created successfully.") }
                .onFailure { onMessage(it.message ?: "Could not create backup.") }
            busy = false
        }
    }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching { backupManager.restoreBackup(uri) }
                .onSuccess {
                    onMessage("Restore complete. Close and reopen Jetnote to load the restored database.")
                }
                .onFailure { onMessage(it.message ?: "Could not restore this backup.") }
            busy = false
        }
    }

    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
            Text(
                "Export your encrypted database and supported media into a ZIP file, " +
                    "or restore a previously created Jetnote backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { createBackup.launch("jetnote-backup.zip") },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create backup")
                }
                OutlinedButton(
                    onClick = { restoring = true },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore")
                }
            }
            if (busy) {
                Text("Working…", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (restoring) {
        AlertDialog(
            onDismissRequest = { restoring = false },
            title = { Text("Replace local Jetnote data?") },
            text = {
                Text(
                    "Restoring replaces the current database and supported media. " +
                        "Create a backup of your current data first. Jetnote must be closed " +
                        "and reopened after restoration."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoring = false
                        openBackup.launch(arrayOf("application/zip", "application/octet-stream"))
                    }
                ) { Text("Choose backup") }
            },
            dismissButton = {
                TextButton(onClick = { restoring = false }) { Text("Cancel") }
            }
        )
    }
}
