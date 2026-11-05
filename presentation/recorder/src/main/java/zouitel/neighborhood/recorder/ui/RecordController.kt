package zouitel.neighborhood.recorder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ui.CommonIcons.MIC_36
import zouitel.neighborhood.ui.CommonIcons.PAUSE
import zouitel.neighborhood.ui.CommonIcons.PAUSE_36
import zouitel.neighborhood.ui.CommonIcons.PLAY_36
import zouitel.neighborhood.ui.CommonIcons.STOP_36
import zouitel.neighborhood.ui.CommonRow

@Composable
fun RecordController(
    recorderModel: RecorderScreenModel,
    onStart: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {}
) {
    val uiState by remember(recorderModel, recorderModel::uiState).collectAsState()

    Row {
        CommonRow(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = !uiState.isRecording,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                IconButton(
                    onClick = onStart,
                    enabled = !uiState.isStarting,
                    modifier = Modifier.sizeIn(50.dp)
                ) {
                    Icon(
                        painter = painterResource(id = MIC_36),
                        contentDescription = "Start recording",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isRecording,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                if (uiState.isPause) {
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier.sizeIn(50.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = PLAY_36),
                            contentDescription = "Resume recording",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier.sizeIn(50.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = PAUSE_36),
                            contentDescription = "Pause recording",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isRecording,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.sizeIn(50.dp)
                ) {
                    Icon(
                        painter = painterResource(id = STOP_36),
                        contentDescription = "Stop recording",
                        tint = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }
        }
    }
}