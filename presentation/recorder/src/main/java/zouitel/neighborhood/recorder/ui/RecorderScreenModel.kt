package zouitel.neighborhood.recorder.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.recorder.recorder.RecorderRepo
import zouitel.neighborhood.recorder.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
@Suppress("DeferredResultUnused")
class RecorderScreenModel(private val repo: RecorderRepo): ScreenModel {
    var time: Duration by mutableStateOf(Duration.ZERO)

    private val timer = mutableStateOf<Timer?>(null)

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
        .withFlow(UiState())

    fun start(file: File) {
        withAsync {
            launch { repo.start(file) }
            launch {
                timer.value = fixedRateTimer(initialDelay = 1000L, period = 1000L) {
                    time += 1.seconds
                    updateTimeStates()
                }
            }
            _uiState.value = _uiState.value.copy(isRecording = true)
        }
    }

    fun pause() {
        repo.pause()
        timer.value?.cancel()
        _uiState.value = _uiState.value.copy(isPause = true)
    }

    fun resume() {
        repo.resume()
        timer.value = fixedRateTimer(initialDelay = 1000L, period = 1000L) {
            time += 1.seconds
            updateTimeStates()
        }
        _uiState.value = _uiState.value.copy(isPause = false)
    }

    fun stop() {
        withAsync {
            launch { repo.stop() }
            launch {
                _uiState.value = _uiState.value.copy(isRecording = false)
            }
        }
    }

    fun reset() {
        timer.value?.cancel()
        time = Duration.ZERO
        updateTimeStates()
    }

    private fun updateTimeStates() {
        time.toComponents { hours, minutes, seconds, _ ->
            _uiState.value = _uiState.value.copy(
                seconds = seconds.pad(),
                minutes = minutes.pad(),
                hours = hours.toInt().pad()
            )
        }
    }

    private fun Int.pad(): String {
        return this.toString().padStart(2, '0')
    }

    override fun onDispose() {
        super.onDispose()
        stop()
    }
}