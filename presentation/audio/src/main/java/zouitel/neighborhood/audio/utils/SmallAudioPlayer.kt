package zouitel.neighborhood.audio.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.ui.CommonConstants.LIST
import zouitel.neighborhood.ui.CommonIcons.PAUSE
import zouitel.neighborhood.ui.CommonIcons.PLAY
import zouitel.neighborhood.ui.CommonRow
import zouitel.neighborhood.ui.SettingsScreenModel
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun SmallAudioPlayer(
    size: Int,
    settingsModel: SettingsScreenModel,
    audioModel: AudioScreenModel,
    audio: Audio
) {
    LaunchedEffect(true) {
        audioModel.initController()
    }
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    val uiState by remember(audioModel, audioModel::uiState).collectAsState()

    Row {
        CommonRow {
            Card(
                modifier = Modifier.padding(10.dp),
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(.6f, .6f, .6f, .5f)
                )
            ) {
                Row {
                    CommonRow(
                        modifier = Modifier
                            .padding(start = 5.dp, end = 5.dp)
                            .height(40.dp)
                    ) {
                        if (settings.layout == LIST) {
                            IconButton({
                                audioModel.initAudio(audio)
                                audioModel.playbackItemState(audio)
                            }) {
                                Icon(
                                    painter = painterResource(if (audio.path == uiState.currentPath && uiState.isPlaying) PAUSE else PLAY),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            LinearProgressIndicator(
                                progress = { if (audio.path == uiState.currentPath) uiState.progress else 0f },
                                modifier = Modifier.padding(7.dp),
                                trackColor = Color.White,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )

                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                IconButton({
                                    if (audio.path == uiState.currentPath) {
                                        audioModel.playbackItemState(audio)
                                    } else {
                                        audioModel.initAudio(audio)
                                        audioModel.playbackItemState(audio)
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(if (audio.path == uiState.currentPath && uiState.isPlaying) PAUSE else PLAY),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.surfaceBright
                                    )
                                }

                                CircularProgressIndicator(
                                    progress = { if (audio.path == uiState.currentPath) uiState.progress else 0f },
                                    trackColor = Color.Transparent,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }

                        Text(
                            modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                            text = formatLong(audio.duration),
                            color = MaterialTheme.colorScheme.surfaceBright
                        )
                    }
                }
            }
            if (size > 1) {
                Text(
                    text = "+${size - 1}",
                    color = MaterialTheme.colorScheme.surfaceBright
                )
            }
        }
    }

}

internal fun formatLong(value: Long): String {
    val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
    return dateFormat.format(value)
}