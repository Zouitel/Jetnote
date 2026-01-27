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
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import zouitel.neighborhood.audio.model.Audio
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

class PlaybackManager (
    private val context: Context,
    private val playService: MediaSessionService
): Player.Listener {

    companion object {
        private const val PLAYER_POSITION_UPDATE_TIME = 500L
    }

    val events: MutableSharedFlow<Event> = MutableSharedFlow()

    private var lastEmittedPosition: Long = 0
    private var playlist: MutableList<MediaItem> = mutableListOf() // Use a list for multiple items
    private var currentMediaItem: MediaItem? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private var handler: Handler? = null

    private val playerPositionRunnable = object : Runnable {
            override fun run() {
                val playbackPosition = controller?.currentPosition ?: 0

                // Emit only new player position
                if (playbackPosition != lastEmittedPosition) {
                    sendEvent(Event.PositionChanged(playbackPosition))
                    lastEmittedPosition = playbackPosition
                }
                sendEvent(Event.PlayingChanged(controller?.isPlaying == true))
                sendEvent(Event.IsLoading(controller?.isLoading == false))
                handler?.postDelayed(this, PLAYER_POSITION_UPDATE_TIME)
            }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        sendEvent(Event.CurrentPath(currentMediaItem?.mediaId ?: ""))
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        val item = getMediaItem(currentMediaItem?.mediaId?.toUri() ?: Uri.EMPTY, "")
        val index = playlist.indexOf(item)

        if (index != -1 && mediaItem?.mediaId == currentMediaItem?.mediaId) {
            controller?.seekTo(index, 0)
            controller?.prepare()
            controller?.play()
        } else {
            controller?.stop()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_ENDED) {
            controller?.stop()
            controller?.seekToDefaultPosition()
        }
    }

    private fun clearAudio() {
        controller?.stop()
        controller?.clearMediaItems()
        currentMediaItem = null
    }

    fun addAudio(audio: Audio) {
        val mediaItem = getMediaItem(audio.path.toUri(), audio.title)
        playlist.add(mediaItem)
        controller?.addMediaItem(mediaItem)
    }

    fun addAudios(items: List<Audio>) {
        val records = items.map { getMediaItem(it.path.toUri(), it.title) }
        playlist.addAll(records)
        controller?.addMediaItems(records)
    }

    fun removeAudio(audio: Audio) {
        val mediaItem = getMediaItem(audio.path.toUri(), audio.title)
        val index = playlist.indexOf(mediaItem)
        if (index != -1) {
            playlist.removeAt(index)
            controller?.removeMediaItem(index)
        }
    }

    fun clearPlaylist() {
        playlist.clear()
        controller?.clearMediaItems()
    }


    fun initializeController() {
        controllerFuture = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, playService::class.java))
        ).buildAsync()
        controllerFuture?.addListener(::onControllerCreated, MoreExecutors.directExecutor())
    }

    fun releaseController() {
        clearAudio()
        controller?.removeListener(this)
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        handler?.removeCallbacks(playerPositionRunnable)
        handler = null

        playlist.clear()
        controller?.clearMediaItems()
    }

    // todo: create di.
    private fun getMediaItem(uri: Uri, title: String): MediaItem {
        val mmd = MediaMetadata.Builder()
            .setTitle(title)
            .build()
        val rmd = MediaItem.RequestMetadata.Builder()
            .setMediaUri(uri)
            .build()
        return MediaItem.Builder()
            .setMediaId(uri.toString())
            .setMediaMetadata(mmd)
            .setRequestMetadata(rmd)
            .build()
    }

    private fun onControllerCreated() {
        // Add all media items to the controller
        controller?.addMediaItems(playlist)
        handler = Handler(Looper.getMainLooper())
        handler?.postDelayed(playerPositionRunnable, PLAYER_POSITION_UPDATE_TIME)
        controller?.addListener(this)
    }

    suspend fun playItem(audio: Audio) {
        val mediaItem = getMediaItem(audio.path.toUri(), audio.title)
        val index = playlist.indexOf(mediaItem)
        currentMediaItem = mediaItem

        if (index != -1) {
            controller?.seekTo(
                index,
                if (audio.path == currentMediaItem?.mediaId) lastEmittedPosition else 0
            )
            controller?.setMediaItem(mediaItem)
            controller?.prepare()
            delay(50)
            controller?.play()
        }
    }

    suspend fun pauseItem(audio: Audio) {
        if(audio.path == currentMediaItem?.mediaId) {
            controller?.pause()
        } else {
            playItem(audio)
        }
    }

    private fun sendEvent(event: Event) {
        runBlocking { events.emit(event) }
    }

    sealed interface Event {
        data class PositionChanged(val position: Long) : Event
        data class PlayingChanged(val isPlaying: Boolean) : Event
        data class IsLoading(val isLoading: Boolean): Event
        data class CurrentPath(val path: String): Event
    }
}