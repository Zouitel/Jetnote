package zouitel.neighborhood.workspace.ui.workplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.audio.utils.LargeAudioPlayer
import zouitel.neighborhood.audio.utils.MediumAudioPlayer
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.ui.features.RecorderSheet

/**
 * Renders the existing audio player and recorder UI as document-owned blocks.
 * Audio files remain stored in Data.audios, so they continue to use the existing
 * persistence and audio playback implementation.
 */
@Composable
internal fun AudioObjectBlock(
    data: MutableState<Data>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val playerModel = koinInject<AudioScreenModel>()
    val scope = rememberCoroutineScope()
    val uiState by remember(playerModel, playerModel::uiState).collectAsState()

    LaunchedEffect(playerModel) {
        playerModel.initController()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        data.value.audios.forEach { audio ->
            val onDelete: () -> Unit = {
                data.value = data.value.copy(audios = data.value.audios - audio)
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "${audio.title} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        data.value = data.value.copy(audios = data.value.audios + audio)
                    }
                }
            }
            if (audio.isRecorded) {
                MediumAudioPlayer(playerModel, audio, onDelete)
            } else {
                LargeAudioPlayer(playerModel, audio, onDelete)
            }
        }
    }
}

@Composable
internal fun RecorderObjectBlock(
    data: MutableState<Data>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RecorderSheet(data = data)
    }
}
