package zouitel.neighborhood.workspace.ui.features

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ContextualFlowColumn
import androidx.compose.foundation.layout.ContextualFlowColumnOverflow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.audio.utils.LargeAudioPlayer
import zouitel.neighborhood.audio.utils.MediumAudioPlayer
import zouitel.neighborhood.workspace.model.Data
import kotlinx.coroutines.launch

class AudioScreen(val data: MutableState<Data>, val snackbarHostState: SnackbarHostState): Screen {
    @Composable
    override fun Content() {
        val playerModel = getScreenModel<AudioScreenModel>()
        val uiState by remember(playerModel, playerModel::uiState).collectAsState()

        val scope = rememberCoroutineScope()
        val audios = data.value.audios
        var audioUri by remember { mutableStateOf<Uri?>(uiState.currentPath.toUri()) }
        var isLoading by remember { mutableStateOf(true) }
        var columnHeight by remember { mutableStateOf(300.dp) }
        val deletedAudios = remember { mutableStateListOf<Pair<Audio, Int>>() }
        val jobCounter = remember { mutableIntStateOf(0) }

        LaunchedEffect(true) {
            playerModel.initController()

            if (audioUri != null && isLoading) {
                isLoading = false
            }
        }

        ContextualFlowColumn(
            modifier = Modifier.heightIn(0.dp, columnHeight),
            itemCount = audios.size,
            maxLines = 1,
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center,
            itemHorizontalAlignment = Alignment.CenterHorizontally,
            overflow = ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                expandIndicator = {
                    ElevatedFilterChip(
                        selected = true,
                        shape = CircleShape,
                        onClick = { columnHeight = Dp.Unspecified },
                        label = {
                            Text(
                                text = "+${totalItemCount - shownItemCount} more",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                },
                collapseIndicator = {
                    if (columnHeight == Dp.Unspecified) {
                        ElevatedFilterChip(
                            selected = true,
                            shape = CircleShape,
                            onClick = { columnHeight = 300.dp },
                            label = {
                                Text(
                                    text = "less",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }
            )
        ) { index ->
            val item = audios.getOrNull(index)
            if (item?.isRecorded == true) {
                MediumAudioPlayer(playerModel, item) {
                    val currentJobId = jobCounter.intValue++
                    deletedAudios.add(Pair(item, currentJobId))
                    val audioPair = deletedAudios.firstOrNull { it.second == currentJobId }

                    data.value = data.value.copy(
                        audios = data.value.audios.minusElement(audioPair?.first ?: return@MediumAudioPlayer)
                    )

                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = "${audioPair.first.title} deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Long
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            audioPair.first.let { audio ->
                                data.value = data.value.copy(
                                    audios = data.value.audios.plusElement(audio)
                                )
                            }
                        }
                        audioPair.let { deletedAudios.remove(it) }
                    }
                }
            } else {
                if (item != null) {
                    LargeAudioPlayer(playerModel, item) {
                        val currentJobId = jobCounter.intValue++
                        deletedAudios.add(Pair(item, currentJobId))
                        val audioPair = deletedAudios.firstOrNull { it.second == currentJobId }
                        data.value = data.value.copy(
                            audios = data.value.audios.minusElement(audioPair?.first ?: return@LargeAudioPlayer)
                        )

                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = "${audioPair.first.title} deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Long
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                audioPair.first.let { audio ->
                                    data.value = data.value.copy(
                                        audios = data.value.audios.plusElement(audio)
                                    )
                                }
                            }
                            audioPair.let { deletedAudios.remove(it) }
                        }
                    }
                }
            }
        }
    }
}
