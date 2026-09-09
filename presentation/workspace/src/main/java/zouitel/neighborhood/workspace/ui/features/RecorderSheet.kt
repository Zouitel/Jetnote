package zouitel.neighborhood.workspace.ui.features

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.recorder.ui.RecordController
import zouitel.neighborhood.recorder.ui.RecordTimer
import zouitel.neighborhood.recorder.ui.RecorderScreenModel
import zouitel.neighborhood.workspace.model.Data
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@Composable
fun RecorderSheet(
    data: MutableState<Data>,
    onRecordingComplete: (() -> Unit)? = null
) {
    val recorderModel = koinInject<RecorderScreenModel>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by remember(recorderModel, recorderModel::uiState).collectAsState()

    // Create a stable file reference that persists across recompositions
    val audioFile = remember {
        val randomLong: Long = Random.nextLong()
        // Get the "Podcasts" directory relative to your app's external storage
        val podcastDir = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)

        // Ensure the directory exists
        if (podcastDir?.exists() == false) {
            podcastDir.mkdirs()
        }

        File(podcastDir, "$randomLong.mp3")    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            // Ensure recorder is stopped when the composable is disposed
            if (uiState.isRecording) {
                recorderModel.stop()
                recorderModel.reset()
            }
        }
    }

    // Prevent the sheet from being dismissed while recording is active
    BackHandler(enabled = uiState.isRecording) { /* consume back press — do nothing */ }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            RecordTimer(uiState.seconds, uiState.minutes, uiState.hours)

            RecordController(
                recorderModel = recorderModel,
                onStart = {
                    recorderModel.start(audioFile)
                },
                onPause = recorderModel::pause,
                onResume = recorderModel::resume,
                onStop = {
                    scope.launch {
                        // Stop the recorder
                        recorderModel.stop()

                        // Small delay to ensure file is written
                        delay(200.milliseconds)

                        // Verify file exists before adding to data
                        if (audioFile.exists()) {
                            val audio = Audio(
                                id = audioFile.nameWithoutExtension.toLongOrNull() ?: Random.nextLong(),
                                path = audioFile.absolutePath,
                                isRecorded = true,
                                duration = recorderModel.time.toLong(DurationUnit.MILLISECONDS),
                            )

                            data.value = data.value.copy(
                                audios = data.value.audios.plusElement(audio)
                            )
                        }

                        // Reset recorder state
                        recorderModel.reset()

                        // Notify completion
                        delay(200.milliseconds)
                        onRecordingComplete?.invoke()
                    }
                }
            )

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}