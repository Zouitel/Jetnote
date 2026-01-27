package zouitel.neighborhood.video

import android.graphics.Color
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(uri: Uri, onLongClock:(BottomSheetNavigator) -> Unit, onClick:(BottomSheetNavigator) -> Unit) {
    val context = LocalContext.current
    val bottomNav = LocalBottomSheetNavigator.current
    val exoPlayer = remember(context, uri) { // Add uri as a key
        ExoPlayer.Builder(context).build().apply {
            try {
                val mediaMetadata = MediaMetadata.Builder()
                    .build()

                // Load the local URI using ContentResolver
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(mediaMetadata)
                    .build()
                setMediaItem(mediaItem)

                // Make sure to use PREPARE_WAIT_FOR_SURFACE_REQUEST to
                // prevent crashing in Compose
                prepare()
                playWhenReady = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true

                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) // Show buffering indicator
                setBackgroundColor(Color.BLACK)

                // Customize the controller
                setShowFastForwardButton(false)  // Remove fast forward
                setShowRewindButton(false) // remove rewind
                setShowNextButton(false) // remove next button
                setShowPreviousButton(false) // remove prev button
                setControllerAnimationEnabled(false)
                setFullscreenButtonClickListener {
                    player?.pause()
                    onClick(bottomNav)
                }
                setOnLongClickListener {
                    player?.pause()
                    onLongClock(bottomNav)
                    true
                }
            }
        }, update = { playerView ->
            playerView.player = exoPlayer
        })

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}