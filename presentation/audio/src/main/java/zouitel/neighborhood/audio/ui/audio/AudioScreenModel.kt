package zouitel.neighborhood.audio.ui.audio

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.audio.audio.AudioRepository
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.player.PlaybackManager
import zouitel.neighborhood.audio.state.UiState
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

@Stable
@Suppress("DeferredResultUnused")
class AudioScreenModel(
    private val player: PlaybackManager,
    private val repo: AudioRepository
): ScreenModel {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
        .withFlow(UiState())

    private val _observeByUid: MutableStateFlow<List<Audio>> = MutableStateFlow(emptyList())
    val observeByUid: StateFlow<List<Audio>> = _observeByUid
        .withFlow(emptyList())

    private val _observeAll: MutableStateFlow<List<Audio>> = MutableStateFlow(emptyList())
    val observeAll: StateFlow<List<Audio>> = _observeAll
        .withFlow(emptyList())

    private val currentAudio = MutableStateFlow<Audio?>(null)
    private val audioByPath = mutableMapOf<String, Audio>()

    init {
        withAsync { observePlaybackEvents() }
    }

    fun initAudio(audio: Audio) {
        audioByPath[audio.path] = audio
        withAsync {
            player.addAudio(audio)
        }
    }

    fun sendAction(act: Action) {
        when(act) {
            is Action.Delete<*> -> withAsync {
                player.removeAudio(act.data as Audio)
            }
            else -> throw Exception("Action not supported")
        }
    }

    fun initController() {
        player.initializeController()
    }

    fun playbackItemState(item: Audio) {
        audioByPath[item.path] = item
        currentAudio.value = item
        withAsync {
            when {
                _uiState.value.isPlaying -> player.pauseItem(item)
                else -> player.playItem(item)
            }
        }
    }

    private suspend fun observePlaybackEvents() {
        player.events.collectLatest {
            when (it) {
                is PlaybackManager.Event.PositionChanged -> updatePlaybackProgress(it.position)
                is PlaybackManager.Event.PlayingChanged -> updatePlayingState(it.isPlaying)
                is PlaybackManager.Event.IsLoading -> updateLoadingState(it.isLoading)
                is PlaybackManager.Event.CurrentPath -> {
                    updateCurrentPath(it.path)
                    audioByPath[it.path]?.let { audio -> currentAudio.value = audio }
                }
            }
        }
    }

    fun loadAudioThump(filePath: String): Bitmap? {
        return repo.loadAudioThumbnail(filePath)
    }

    fun updatePlaybackProgress(position: Long) {
        val record = currentAudio.value ?: return
        if (record.duration <= 0L) return
        _uiState.value = uiState.value.copy(
            progress = (position.toFloat() / record.duration).coerceIn(0f, 1f)
        )
    }

    private fun updatePlayingState(isPlaying: Boolean) {
        _uiState.value = uiState.value.copy(isPlaying = isPlaying)
    }

    private fun updateLoadingState(isLoading: Boolean) {
        _uiState.value = uiState.value.copy(isLoading = isLoading)
    }

    private fun updateCurrentPath(path: String) {
        _uiState.value = uiState.value.copy(currentPath = path)
    }

}