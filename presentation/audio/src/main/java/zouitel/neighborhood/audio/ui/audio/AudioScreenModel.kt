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
) : ScreenModel {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.withFlow(UiState())

    private val _observeByUid: MutableStateFlow<List<Audio>> = MutableStateFlow(emptyList())
    val observeByUid: StateFlow<List<Audio>> = _observeByUid.withFlow(emptyList())

    private val _observeAll: MutableStateFlow<List<Audio>> = MutableStateFlow(emptyList())
    val observeAll: StateFlow<List<Audio>> = _observeAll.withFlow(emptyList())

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
        val isCurrentTrack = _uiState.value.currentPath == item.path
        audioByPath[item.path] = item
        currentAudio.value = item
        _uiState.value = _uiState.value.copy(
            playingAudio = item,
            progress = if (isCurrentTrack) _uiState.value.progress else 0f
        )
        withAsync {
            if (isCurrentTrack && _uiState.value.isPlaying) {
                player.pauseItem(item)
            } else {
                player.playItem(item)
            }
        }
    }

    private suspend fun observePlaybackEvents() {
        player.events.collectLatest { event ->
            when (event) {
                is PlaybackManager.Event.PositionChanged -> updatePlaybackProgress(event.position)
                is PlaybackManager.Event.PlayingChanged -> updatePlayingState(event.isPlaying)
                is PlaybackManager.Event.IsLoading -> updateLoadingState(event.isLoading)
                is PlaybackManager.Event.CurrentPath -> updateCurrentAudio(event.path)
            }
        }
    }

    private fun updateCurrentAudio(path: String) {
        if (_uiState.value.currentPath == path) return

        val audio = audioByPath[path]
        currentAudio.value = audio
        _uiState.value = _uiState.value.copy(
            currentPath = path,
            playingAudio = audio,
            progress = 0f,
            isPlaying = if (path.isBlank()) false else _uiState.value.isPlaying
        )
    }

    fun loadAudioThump(filePath: String): Bitmap? = repo.loadAudioThumbnail(filePath)

    fun updatePlaybackProgress(position: Long) {
        val record = currentAudio.value ?: return
        if (record.duration <= 0L) return
        _uiState.value = _uiState.value.copy(
            progress = (position.toFloat() / record.duration).coerceIn(0f, 1f)
        )
    }

    private fun updatePlayingState(isPlaying: Boolean) {
        _uiState.value = uiState.value.copy(isPlaying = isPlaying)
    }

    private fun updateLoadingState(isLoading: Boolean) {
        _uiState.value = uiState.value.copy(isLoading = isLoading)
    }

}