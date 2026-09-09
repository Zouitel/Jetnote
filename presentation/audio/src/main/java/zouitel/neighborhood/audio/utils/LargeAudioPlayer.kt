package zouitel.neighborhood.audio.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonIcons.PAUSE
import zouitel.neighborhood.ui.CommonIcons.PLAY
import zouitel.neighborhood.ui.CommonRow
import zouitel.neighborhood.waveform.Waveform
import zouitel.neighborhood.waveform.model.AmplitudeType
import zouitel.neighborhood.waveform.model.WaveformAlignment
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LargeAudioPlayer(playerModel: AudioScreenModel, item: Audio, onDelete: () -> Unit) {
    val uiState by remember(playerModel, playerModel::uiState).collectAsState()

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxSwipeDistance = -150f

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(offsetX.value != 0f) {
                    IconButton(
                        modifier = Modifier.padding(horizontal = 5.dp),
                        onClick = {
                            scope.launch {
                                offsetX.animateTo(0f, tween(300))
                                delay(100.milliseconds)
                                onDelete()
                            }
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(CommonIcons.TRASH),
                            contentDescription = "Delete"
                        )
                    }
                }
            }
            Card(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .graphicsLayer {
                        translationX = offsetX.value
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (offsetX.value < maxSwipeDistance / 2) {
                                        offsetX.animateTo(maxSwipeDistance, tween(300))
                                    } else {
                                        offsetX.animateTo(0f, tween(300))
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    val newOffset = (offsetX.value + dragAmount)
                                        .coerceIn(maxSwipeDistance, 0f)
                                    offsetX.snapTo(newOffset)
                                }
                            }
                        )
                    }
                    .height(90.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(.6f, .6f, .6f, .1f)
                )
            ) {
                Box {
                    playerModel.loadAudioThump(item.path)?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                    Column {
                        Row {
                            CommonRow(
                                modifier = Modifier.background(Color(.6f, .6f, .6f, .5f))
                                    .height(60.dp)
                            ) {
                                IconButton({
                                    playerModel.initAudio(item)
                                    playerModel.playbackItemState(item)
                                }) {
                                    Icon(
                                        painter = painterResource(if (item.path == uiState.currentPath && uiState.isPlaying) PAUSE else PLAY),
                                        null,
                                        modifier = Modifier.padding(5.dp),
                                        tint = Color.Companion.White
                                    )
                                }
                                Waveform(
                                    modifier = Modifier.weight(1f),
                                    amplitudes = item.amplitude.map { it.digitToInt() },
                                    progress = if (item.path == uiState.currentPath) uiState.progress else 0f,
                                    onProgressChange = { playerModel::updatePlaybackProgress },
                                    waveformAlignment = WaveformAlignment.Center,
                                    amplitudeType = AmplitudeType.Avg,
                                    style = Fill,
                                    progressBrush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF136FC3),
                                            Color(0xFF76EF66)
                                        )
                                    ),
                                    spikePadding = 3.dp,
                                    spikeRadius = 3.dp,
                                    spikeWidth = 3.dp,
                                )
                            }
                        }
                        Row {
                            CommonRow(
                                modifier = Modifier
                                    .height(30.dp)
                                    .weight(1f)
                                    .background(Color(.6f, .6f, .6f, .5f))

                            ) {
                                Text(text = item.nameWithoutFormat, color = Color.White)

                                Text(
                                    modifier = Modifier.padding(end = 10.dp),
                                    text = formatLong(item.duration),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}