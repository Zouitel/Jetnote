package zouitel.neighborhood.recorder.ui

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.recorder.recorder.RecorderRepo
import zouitel.neighborhood.recorder.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@Stable
class RecorderScreenModel(
    private val repo: RecorderRepo
) : ScreenModel {
    private val screenScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val operationMutex = Mutex()
    private var timerJob: Job? = null
    private var elapsedSeconds = 0

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(UiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<UiState> = _uiState

    fun start(file: File) {
        screenScope.launch {
            operationMutex.withLock {
                if (_uiState.value.isRecording || _uiState.value.isStarting) return@withLock

                timerJob?.cancel()
                elapsedSeconds = 0
                _uiState.value = UiState(isStarting = true)
                runCatching {
                    withContext(Dispatchers.IO) { repo.start(file) }
                }.onSuccess {
                    _uiState.value = UiState(isRecording = true)
                    startTimer()
                }.onFailure { error ->
                    _uiState.value = UiState(errorMessage = error.message ?: "Unable to start recording")
                }
            }
        }
    }

    fun pause() {
        screenScope.launch {
            operationMutex.withLock {
                if (!_uiState.value.isRecording || _uiState.value.isPause) return@withLock
                runCatching {
                    withContext(Dispatchers.IO) { repo.pause() }
                }.onSuccess {
                    timerJob?.cancel()
                    timerJob = null
                    _uiState.value = _uiState.value.copy(isPause = true)
                }.onFailure(::publishError)
            }
        }
    }

    fun resume() {
        screenScope.launch {
            operationMutex.withLock {
                if (!_uiState.value.isRecording || !_uiState.value.isPause) return@withLock
                runCatching {
                    withContext(Dispatchers.IO) { repo.resume() }
                }.onSuccess {
                    _uiState.value = _uiState.value.copy(isPause = false)
                    startTimer()
                }.onFailure(::publishError)
            }
        }
    }

    fun stop() {
        screenScope.launch {
            operationMutex.withLock {
                if (!_uiState.value.isRecording && !_uiState.value.isStarting) return@withLock
                timerJob?.cancel()
                timerJob = null
                runCatching {
                    withContext(Dispatchers.IO) { repo.stop() }
                }.onSuccess {
                    // Keep the final elapsed duration available until the caller saves the recording.
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        isPause = false,
                        isStarting = false,
                    )
                }.onFailure { error ->
                    _uiState.value = UiState(errorMessage = error.message ?: "Unable to stop recording")
                }
            }
        }
    }

    fun reset() {
        if (_uiState.value.isRecording) return
        timerJob?.cancel()
        timerJob = null
        elapsedSeconds = 0
        _uiState.value = UiState()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = screenScope.launch {
            while (isActive) {
                delay(1_000)
                elapsedSeconds += 1
                val hours = elapsedSeconds / 3_600
                val minutes = (elapsedSeconds % 3_600) / 60
                val seconds = elapsedSeconds % 60
                _uiState.value = _uiState.value.copy(
                    hours = hours.pad(),
                    minutes = minutes.pad(),
                    seconds = seconds.pad(),
                    elapsedMilliseconds = elapsedSeconds * 1_000L
                )
            }
        }
    }

    private fun publishError(error: Throwable) {
        _uiState.value = _uiState.value.copy(
            errorMessage = error.message ?: "Recording operation failed"
        )
    }

    private fun Int.pad(): String = toString().padStart(2, '0')

    override fun onDispose() {
        timerJob?.cancel()
        screenScope.cancel()
        // MediaRecorder must be released even if the screen is removed mid-recording.
        runCatching { repo.stop() }
        super.onDispose()
    }
}
