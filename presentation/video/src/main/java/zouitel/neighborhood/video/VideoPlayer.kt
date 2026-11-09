package zouitel.neighborhood.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.indicator.PositionText
import androidx.media3.ui.compose.material3.indicator.ProgressSlider
import androidx.media3.ui.compose.material3.indicator.RemainingDurationText
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import zouitel.neighborhood.ui.CommonIcons
import kotlin.math.abs
import kotlin.math.max

/** Playback speed while the user holds a finger on the video. */
private const val FAST_PLAYBACK_SPEED = 2f

// ---- Ambient mode tuning -------------------------------------------------------------------
/** The glow is a COLS x ROWS grid of soft radial gradients, each tinted with the average colour
 *  of the matching part of the current video frame. */
private const val AMBIENT_COLS = 4
private const val AMBIENT_ROWS = 3

/** Frames are sampled tiny; we only need colours, not detail. */
private const val AMBIENT_SAMPLE_W = 32
private const val AMBIENT_SAMPLE_H = 18

/** How often a new frame is sampled while playing, and the minimum position change that
 *  justifies sampling again (so a paused video is not re-sampled forever). */
private const val AMBIENT_INTERVAL_MS = 1_000L
private const val AMBIENT_MIN_STEP_MS = 400L

/** Colour crossfade between two samples. Slightly longer than the interval = smooth, never jumpy. */
private const val AMBIENT_FADE_MS = 1_200

/** How far the glow bleeds past the player's bounds (only visible if the parent doesn't clip). */
private val AmbientBleed = 40.dp

/** The video is inset a little while ambient mode is on, so the glow frames it on every side. */
private val AmbientVideoInset = 10.dp

@Suppress("UNUSED_PARAMETER") // onLongClock is kept so existing call sites still compile
@OptIn(UnstableApi::class, ExperimentalApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    /** Initial state of ambient mode (YouTube-style glow behind the video). */
    ambientMode: Boolean = true,
    /** Called when the user flips the "Ambient" pill, so the caller can persist the choice. */
    onAmbientModeChange: (Boolean) -> Unit = {},
    /** No longer triggered: a long press on the frame now means 2x speed while held. */
    onLongClock: (BottomSheetNavigator) -> Unit,
    /** Opens the full-size player: called by the full-size button and by a double tap on the frame. */
    onClick: (BottomSheetNavigator) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val bottomNav = LocalBottomSheetNavigator.current
    val haptics = LocalHapticFeedback.current
    // pointerInput below is keyed on the player only, so always read the newest callbacks.
    val latestOnClick by rememberUpdatedState(onClick)
    val latestBottomNav by rememberUpdatedState(bottomNav)

    val exoPlayer = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(MediaMetadata.Builder().build())
                    .build()
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // These values describe a specific player and must reset when its media changes.
    var isPlaying by remember(exoPlayer) { mutableStateOf(exoPlayer.isPlaying) }
    var isBuffering by remember(exoPlayer) {
        mutableStateOf(exoPlayer.playbackState == Player.STATE_BUFFERING)
    }
    var hasError by remember(exoPlayer) { mutableStateOf(exoPlayer.playerError != null) }
    var controlsVisible by remember(exoPlayer) { mutableStateOf(true) }
    var showRemainingTime by remember(exoPlayer) { mutableStateOf(false) }

    // Hold-to-fast-forward: true while a long press keeps the player at FAST_PLAYBACK_SPEED.
    var isSpeedBoosted by remember(exoPlayer) { mutableStateOf(false) }
    var normalSpeed by remember(exoPlayer) { mutableStateOf(1f) }

    // Ambient mode: on/off, and the latest colour grid sampled from the video (null = none yet).
    var ambientOn by remember(ambientMode) { mutableStateOf(ambientMode) }
    var ambientColors by remember(exoPlayer) { mutableStateOf<List<Color>?>(null) }

    // Mirror current and future player state into Compose, and release exactly once.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) hasError = false
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                error.printStackTrace()
            }
        }
        exoPlayer.addListener(listener)

        // prepare() may have advanced the player before this listener was attached.
        isPlaying = exoPlayer.isPlaying
        isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
        hasError = exoPlayer.playerError != null

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Pause (never auto-resume) when the screen/app goes into the background.
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3_000)
            controlsVisible = false
        }
    }

    // Ambient sampler: a second, independent decoder (MediaMetadataRetriever) grabs a tiny frame
    // at the player's position about once a second. It never touches the player's own surface,
    // so playback rendering is unchanged. Runs only while ambient mode is on.
    LaunchedEffect(exoPlayer, uri, ambientOn) {
        if (!ambientOn) return@LaunchedEffect

        val retriever = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().also { it.setDataSource(context, uri) }
            }.getOrNull()
        } ?: return@LaunchedEffect // source can't be sampled -> ambient simply stays off

        try {
            var lastSampledMs = Long.MIN_VALUE
            while (true) {
                val positionMs = exoPlayer.currentPosition // LaunchedEffect runs on Main
                val shouldSample = lastSampledMs == Long.MIN_VALUE ||
                        abs(positionMs - lastSampledMs) >= AMBIENT_MIN_STEP_MS

                if (shouldSample) {
                    val grid = withContext(Dispatchers.IO) {
                        runCatching {
                            retriever.getScaledFrameAtTime(
                                positionMs * 1_000L,
                                MediaMetadataRetriever.OPTION_CLOSEST,
                                AMBIENT_SAMPLE_W,
                                AMBIENT_SAMPLE_H
                            )?.let { frame ->
                                frame.toAmbientGrid().also { frame.recycle() }
                            }
                        }.getOrNull()
                    }
                    if (grid != null) {
                        ambientColors = grid
                        lastSampledMs = positionMs
                    }
                }
                delay(AMBIENT_INTERVAL_MS)
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                runCatching { retriever.release() }
            }
        }
    }

    // One animated colour per grid cell. They are State objects read only in the draw phase
    // (inside ambientGlow), so a colour fade redraws the glow without recomposing the player.
    val glowColors: List<State<Color>> = List(AMBIENT_COLS * AMBIENT_ROWS) { index ->
        animateColorAsState(
            targetValue = ambientColors?.getOrNull(index) ?: Color.Transparent,
            animationSpec = tween(AMBIENT_FADE_MS),
            label = "ambientColor$index"
        )
    }
    val glowVisible = ambientOn && ambientColors != null
    val glowIntensity: State<Float> = animateFloatAsState(
        targetValue = if (glowVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "ambientIntensity"
    )
    val videoInset by animateDpAsState(
        targetValue = if (glowVisible) AmbientVideoInset else 0.dp,
        animationSpec = tween(300),
        label = "ambientInset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .ambientGlow(glowColors, glowIntensity)
            .pointerInput(exoPlayer) {
                detectTapGestures(
                    // Runs on every press and suspends until the finger lifts (or the gesture is
                    // cancelled, e.g. a parent starts scrolling): that is where 2x ends.
                    onPress = {
                        tryAwaitRelease()
                        if (isSpeedBoosted) {
                            isSpeedBoosted = false
                            exoPlayer.setPlaybackSpeed(normalSpeed)
                        }
                    },
                    // One tap: pause / resume. Controls are revealed; while playing they auto-hide.
                    onTap = {
                        exoPlayer.togglePlayPause()
                        controlsVisible = true
                    },
                    // Double tap: full-size player, same action as the full-size button.
                    onDoubleTap = {
                        exoPlayer.pause()
                        latestOnClick(latestBottomNav)
                    },
                    // Long press: 2x speed for as long as the finger stays down (only while playing).
                    onLongPress = {
                        if (exoPlayer.isPlaying) {
                            normalSpeed = exoPlayer.playbackParameters.speed
                            exoPlayer.setPlaybackSpeed(FAST_PLAYBACK_SPEED)
                            isSpeedBoosted = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
            .then(if (isPlaying) Modifier.keepScreenOn() else Modifier)
    ) {
        ContentFrame(
            player = exoPlayer,
            modifier = Modifier
                .fillMaxSize()
                .padding(videoInset)
        )

        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(40.dp),
                color = Color.White
            )
        }

        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable {
                        hasError = false
                        exoPlayer.prepare()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(CommonIcons.INFO), null, tint = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Couldn't play video · tap to retry",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSpeedBoosted,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120))
        ) {
            Text(
                text = "2x speed",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            AmbientToggle(
                enabled = ambientOn,
                onToggle = {
                    ambientOn = !ambientOn
                    onAmbientModeChange(ambientOn)
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            IconButton(
                onClick = {
                    exoPlayer.pause()
                    onClick(bottomNav)
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    contentColor = Color.White
                )
            ) {
                Icon(painterResource(CommonIcons.FULL_SIZE), null, tint = Color.White)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        ),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayPauseButton(
                    player = exoPlayer,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
                ProgressSlider(
                    player = exoPlayer,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Red
                    )
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showRemainingTime = !showRemainingTime }
                        )
                ) {
                    if (showRemainingTime) {
                        RemainingDurationText(
                            player = exoPlayer,
                            showNegative = true,
                            color = Color.White
                        )
                    } else {
                        PositionText(
                            player = exoPlayer,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** Pause / resume that also behaves when the video has ended or errored out. */
private fun Player.togglePlayPause() {
    when {
        playbackState == Player.STATE_ENDED -> {
            seekToDefaultPosition()
            play()
        }
        playWhenReady -> pause()
        else -> {
            if (playbackState == Player.STATE_IDLE) prepare()
            play()
        }
    }
}

/** Small pill in the player controls that switches ambient mode on and off. */
@Composable
private fun AmbientToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (enabled) "Ambient · On" else "Ambient · Off",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Draws the ambient glow behind the content: one soft radial gradient per grid cell, laid out over
 * the player's bounds plus [AmbientBleed] on every side. Colours and intensity are read here, in the
 * draw phase, so animating them never recomposes anything.
 */
private fun Modifier.ambientGlow(
    colors: List<State<Color>>,
    intensity: State<Float>
): Modifier = drawBehind {
    val strength = intensity.value
    if (strength <= 0.01f) return@drawBehind

    val bleed = AmbientBleed.toPx()
    val cellW = (size.width + bleed * 2f) / AMBIENT_COLS
    val cellH = (size.height + bleed * 2f) / AMBIENT_ROWS
    val radius = max(cellW, cellH) * 1.2f

    for (index in colors.indices) {
        val color = colors[index].value
        if (color.alpha <= 0.01f) continue

        val center = Offset(
            x = -bleed + cellW * ((index % AMBIENT_COLS) + 0.5f),
            y = -bleed + cellH * ((index / AMBIENT_COLS) + 0.5f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = color.alpha * strength * 0.85f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/** Average colour of each grid cell of this (tiny) frame, slightly boosted so the glow reads well. */
private fun Bitmap.toAmbientGrid(): List<Color> {
    val w = width
    val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    val hsv = FloatArray(3)

    return List(AMBIENT_COLS * AMBIENT_ROWS) { index ->
        val col = index % AMBIENT_COLS
        val row = index / AMBIENT_COLS
        val x0 = col * w / AMBIENT_COLS
        val x1 = max(x0 + 1, (col + 1) * w / AMBIENT_COLS)
        val y0 = row * h / AMBIENT_ROWS
        val y1 = max(y0 + 1, (row + 1) * h / AMBIENT_ROWS)

        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val p = pixels[y * w + x]
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
                count++
            }
        }

        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt()),
            hsv
        )
        hsv[1] = (hsv[1] * 1.35f).coerceAtMost(1f)
        hsv[2] = (hsv[2] * 1.15f).coerceAtMost(1f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }
}