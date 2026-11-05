package zouitel.neighborhood.audio.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import zouitel.neighborhood.audio.model.Audio
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * UI-side controller for the service-owned player.
 *
 * This class never constructs or owns the ExoPlayer. The Android service is
 * responsible for the player and MediaSession lifetime.
 */
class PlaybackManager(private val context: Context) : Player.Listener {

    companion object {
        private const val PLAYER_POSITION_UPDATE_TIME = 500L
        private const val PLAYBACK_SERVICE_CLASS =
            "zouitel.neighborhood.playback.PlaybackService"
    }

    val events = MutableSharedFlow<Event>(extraBufferCapacity = 64)

    private var lastEmittedPosition = 0L
    private val playlist = mutableListOf<MediaItem>()
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val controller: MediaController?
        get() {
            val future = controllerFuture ?: return null
            if (!future.isDone || future.isCancelled) return null
            return runCatching { future.get() }.getOrNull()
        }

    private val handler = Handler(Looper.getMainLooper())
    private var pollingStarted = false

    private val playerPositionRunnable = object : Runnable {
        override fun run() {
            val player = controller ?: run {
                pollingStarted = false
                return
            }

            val position = player.currentPosition
            if (position != lastEmittedPosition) {
                events.tryEmit(Event.PositionChanged(position))
                lastEmittedPosition = position
            }
            events.tryEmit(Event.PlayingChanged(player.isPlaying))
            events.tryEmit(Event.IsLoading(player.isLoading))
            events.tryEmit(Event.CurrentPath(player.currentMediaItem?.mediaId.orEmpty()))
            handler.postDelayed(this, PLAYER_POSITION_UPDATE_TIME)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        events.tryEmit(Event.PlayingChanged(isPlaying))
        events.tryEmit(Event.CurrentPath(controller?.currentMediaItem?.mediaId.orEmpty()))
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lastEmittedPosition = 0L
        events.tryEmit(Event.CurrentPath(mediaItem?.mediaId.orEmpty()))
        events.tryEmit(Event.PositionChanged(0L))
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        events.tryEmit(Event.IsLoading(playbackState == Player.STATE_BUFFERING))
        if (playbackState == Player.STATE_ENDED) {
            events.tryEmit(Event.PlayingChanged(false))
        }
    }

    fun addAudio(audio: Audio) {
        val item = getMediaItem(audio.path.toUri(), audio.title)
        val index = playlist.indexOfFirst { it.mediaId == item.mediaId }
        if (index >= 0) return

        playlist.add(item)
        controller?.addMediaItem(item)
    }

    fun addAudios(items: List<Audio>) {
        items.forEach(::addAudio)
    }

    fun removeAudio(audio: Audio) {
        val mediaId = audio.path
        val index = playlist.indexOfFirst { it.mediaId == mediaId }
        if (index < 0) return

        playlist.removeAt(index)
        controller?.let { player ->
            val playerIndex = (0 until player.mediaItemCount)
                .firstOrNull { player.getMediaItemAt(it).mediaId == mediaId }
            if (playerIndex != null) player.removeMediaItem(playerIndex)
        }

    }

    fun clearPlaylist() {
        playlist.clear()
        lastEmittedPosition = 0L
        controller?.stop()
        controller?.clearMediaItems()
        events.tryEmit(Event.CurrentPath(""))
        events.tryEmit(Event.PlayingChanged(false))
        events.tryEmit(Event.PositionChanged(0L))
    }

    fun initializeController() {
        if (controller != null || controllerFuture?.isDone == false) return

        val token = SessionToken(
            context,
            ComponentName(context.packageName, PLAYBACK_SERVICE_CLASS)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future

        future.addListener(
            {
                handler.post {
                val connected = runCatching { future.get() }.getOrNull()
                if (controllerFuture !== future) {
                    return@addListener
                }

                if (connected == null) {
                    controllerFuture = null
                    events.tryEmit(Event.IsLoading(false))
                    return@addListener
                }

                connected.addListener(this)
                if (playlist.isNotEmpty()) {
                    connected.setMediaItems(playlist.toList(), false)
                }
                events.tryEmit(Event.CurrentPath(connected.currentMediaItem?.mediaId.orEmpty()))
                startPositionUpdates()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun releaseController() {
        stopPositionUpdates()
        val future = controllerFuture
        val connected = controller
        connected?.removeListener(this)
        controllerFuture = null
        if (future != null) {
            MediaController.releaseFuture(future)
        }
    }

    private fun startPositionUpdates() {
        if (pollingStarted) return
        pollingStarted = true
        handler.post(playerPositionRunnable)
    }

    private fun stopPositionUpdates() {
        pollingStarted = false
        handler.removeCallbacks(playerPositionRunnable)
    }

    private fun getMediaItem(uri: Uri, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .build()
        return MediaItem.Builder()
            .setMediaId(uri.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    suspend fun playItem(audio: Audio) {
        val mediaId = audio.path
        val index = playlist.indexOfFirst { it.mediaId == mediaId }
        val player = controller ?: return
        if (index < 0) return

        val resumePosition = if (player.currentMediaItem?.mediaId == mediaId) {
            lastEmittedPosition
        } else {
            0L
        }

        player.seekTo(index, resumePosition)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    suspend fun pauseItem(audio: Audio) {
        val player = controller ?: return
        if (audio.path == player.currentMediaItem?.mediaId) {
            player.pause()
        } else {
            playItem(audio)
        }
    }

    sealed interface Event {
        data class PositionChanged(val position: Long) : Event
        data class PlayingChanged(val isPlaying: Boolean) : Event
        data class IsLoading(val isLoading: Boolean) : Event
        data class CurrentPath(val path: String) : Event
    }
}
