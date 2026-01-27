package zouitel.neighborhood.audio.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonIcons.PAUSE
import zouitel.neighborhood.ui.CommonIcons.PLAY
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediumAudioPlayer(playerModel: AudioScreenModel, item: Audio, onDelete: () -> Unit) {
    val uiState by remember(playerModel, playerModel::uiState).collectAsState()

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxSwipeDistance = -150f

    val animatedProgress by animateFloatAsState(
        targetValue = if (item.path == uiState.currentPath) uiState.progress else 0f,
        animationSpec = tween(durationMillis = 500)
    )

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
                            delay(100)
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
                .fillMaxWidth()
                .wrapContentWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                }
                .height(50.dp)
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
                .padding(horizontal = 10.dp, vertical = 3.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(.6f, .6f, .6f, .5f)
            )
        ) {
            Box {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    color = CommonColors.azure,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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

                    Text(
                        modifier = Modifier.wrapContentWidth().padding(end = 15.dp),
                        text = formatLong(item.duration),
                        color = Color.White
                    )
                }
            }
        }
    }
}