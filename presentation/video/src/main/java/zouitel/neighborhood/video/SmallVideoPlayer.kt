package zouitel.neighborhood.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame

@UnstableApi
@Composable
fun SmallVideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val mediaMetadata = MediaMetadata.Builder().build()
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                setMediaItem(mediaItem)
                prepare()
                volume = 0f
                playbackParameters = PlaybackParameters(0.5f)
                playWhenReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    ContentFrame(
        player = exoPlayer,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        shutter = { Box(Modifier.fillMaxSize().background(Color.Black)) }
    )

    // Release the exact player instance owned by this composition.
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // Muted inline playback should not continue decoding while the app is backgrounded.
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            exoPlayer.pause()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
