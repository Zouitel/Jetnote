package zouitel.neighborhood.navigator.settings_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zouitel.neighborhood.plugin.FeatureInstallState
import zouitel.neighborhood.plugin.PlayFeatureInstaller

private const val SPEECH_MODULE = "speech-transcription"

@Composable
internal fun SpeechTranscriptionPluginPreference() {
    val context = LocalContext.current
    val installer = remember(context) { PlayFeatureInstaller(context) }
    var state by remember(installer) {
        mutableStateOf<FeatureInstallState>(
            if (installer.isInstalled(SPEECH_MODULE)) {
                FeatureInstallState.Installed
            } else {
                FeatureInstallState.NotInstalled
            }
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Subscribe before the user starts an install so no early Play state is missed.
    LaunchedEffect(installer) {
        installer.observeInstall(SPEECH_MODULE).collect { state = it }
    }

    val busy = state is FeatureInstallState.Pending ||
        state is FeatureInstallState.Downloading ||
        state is FeatureInstallState.Installing

    val description = when (val current = state) {
        FeatureInstallState.NotInstalled -> "Install optional offline speech transcription."
        FeatureInstallState.Installed -> "Feature installed. Speech models are downloaded separately."
        is FeatureInstallState.Pending -> "Waiting for Google Play…"
        is FeatureInstallState.Downloading ->
            if (current.totalBytesToDownload > 0L) {
                "Downloading feature: ${current.bytesDownloaded / 1_048_576} / ${current.totalBytesToDownload / 1_048_576} MB"
            } else {
                "Downloading feature…"
            }
        is FeatureInstallState.Installing -> "Installing feature…"
        is FeatureInstallState.AwaitingUserConfirmation -> "Google Play requires your confirmation."
        is FeatureInstallState.Failed -> "Installation failed (code ${current.errorCode})."
        is FeatureInstallState.Cancelled -> "Installation cancelled."
    }

    ListItem(
        headlineContent = {
            Text("Speech & Transcription", style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Column {
                Text(description)
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        trailingContent = {
            when {
                busy -> CircularProgressIndicator()
                state is FeatureInstallState.Installed -> {
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching { installer.uninstall(SPEECH_MODULE) }
                                    .onSuccess { state = FeatureInstallState.NotInstalled }
                                    .onFailure { errorMessage = it.message ?: "Could not remove feature." }
                            }
                        }
                    ) {
                        Text("Remove")
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            errorMessage = null
                            scope.launch {
                                runCatching { installer.install(SPEECH_MODULE) }
                                    .onSuccess { session ->
                                        state = if (session < 0) {
                                            FeatureInstallState.Installed
                                        } else {
                                            FeatureInstallState.Pending(session)
                                        }
                                    }
                                    .onFailure {
                                        errorMessage = it.message ?: "Could not install feature."
                                    }
                            }
                        }
                    ) {
                        Text("Install")
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    )
}
