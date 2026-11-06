package zouitel.neighborhood.waveform

import android.view.MotionEvent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.waveform.model.AmplitudeType
import zouitel.neighborhood.waveform.model.WaveformAlignment

private val MinSpikeWidthDp: Dp = 1.dp
private val MaxSpikeWidthDp: Dp = 24.dp
private val MinSpikePaddingDp: Dp = 0.dp
private val MaxSpikePaddingDp: Dp = 12.dp
private val MinSpikeRadiusDp: Dp = 0.dp
private val MaxSpikeRadiusDp: Dp = 12.dp

private const val MinProgress: Float = 0F
private const val MaxProgress: Float = 1F
private const val MinSpikeHeight: Float = 1F
private const val MaxInputAmplitude: Float = 9F
private const val DefaultGraphicsLayerAlpha: Float = 0.99F

/**
 * Draws an audio waveform and reports normalized playback progress.
 *
 * Amplitude samples are expected to use the 0..9 scale used by the audio module.
 * The modifier controls the component's size; its default minimum height is 48.dp.
 */
@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    style: DrawStyle = Fill,
    waveformBrush: Brush = SolidColor(Color.White),
    progressBrush: Brush = SolidColor(Color.Blue),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.Avg,
    onProgressChangeFinished: (() -> Unit)? = null,
    spikeAnimationSpec: AnimationSpec<Float> = tween(500),
    spikeWidth: Dp = 4.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 1.dp,
    progress: Float = 0F,
    amplitudes: List<Int>,
    onProgressChange: (Float) -> Unit
) {
    val normalizedProgress = progress.coerceIn(MinProgress, MaxProgress)
    val safeSpikeWidth = spikeWidth.coerceIn(MinSpikeWidthDp, MaxSpikeWidthDp)
    val safeSpikePadding = spikePadding.coerceIn(MinSpikePaddingDp, MaxSpikePaddingDp)
    val safeSpikeRadius = spikeRadius.coerceIn(MinSpikeRadiusDp, MaxSpikeRadiusDp)
    val spikeTotalWidth = safeSpikeWidth + safeSpikePadding

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isScrubbing by remember { mutableStateOf(false) }

    val spikeCount = if (spikeTotalWidth > 0.dp) {
        with(LocalDensity.current) {
            (canvasSize.width / spikeTotalWidth.toPx()).toInt().coerceAtLeast(0)
        }
    } else {
        0
    }
    val drawableAmplitudes = remember(
        amplitudes,
        spikeCount,
        amplitudeType,
        canvasSize.height
    ) {
        amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikeCount,
            minHeight = MinSpikeHeight,
            maxHeight = canvasSize.height.toFloat().coerceAtLeast(MinSpikeHeight)
        )
    }
    val animatedAmplitudes = drawableAmplitudes.map { amplitude ->
        animateFloatAsState(
            targetValue = amplitude,
            animationSpec = spikeAnimationSpec
        ).value
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .onSizeChanged { canvasSize = it }
            .graphicsLayer(alpha = DefaultGraphicsLayerAlpha)
            .pointerInteropFilter { event ->
                val width = canvasSize.width.toFloat()
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (width <= 0F || event.x !in 0F..width) {
                            false
                        } else {
                            isScrubbing = true
                            onProgressChange((event.x / width).coerceIn(MinProgress, MaxProgress))
                            true
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!isScrubbing || width <= 0F) {
                            false
                        } else {
                            onProgressChange((event.x / width).coerceIn(MinProgress, MaxProgress))
                            true
                        }
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        if (!isScrubbing) {
                            false
                        } else {
                            isScrubbing = false
                            onProgressChangeFinished?.invoke()
                            true
                        }
                    }

                    else -> false
                }
            }
    ) {
        val spikeWidthPx = safeSpikeWidth.toPx()
        val spikeSpacingPx = spikeTotalWidth.toPx()
        animatedAmplitudes.forEachIndexed { index, amplitude ->
            val top = when (waveformAlignment) {
                WaveformAlignment.Top -> 0F
                WaveformAlignment.Bottom -> size.height - amplitude
                WaveformAlignment.Center -> (size.height - amplitude) / 2F
            }
            drawRoundRect(
                brush = waveformBrush,
                topLeft = Offset(x = index * spikeSpacingPx, y = top),
                size = Size(width = spikeWidthPx, height = amplitude),
                cornerRadius = CornerRadius(safeSpikeRadius.toPx(), safeSpikeRadius.toPx()),
                style = style
            )
        }

        // Apply the progress tint once, after all bars have been drawn.
        drawRect(
            brush = progressBrush,
            size = Size(width = normalizedProgress * size.width, height = size.height),
            blendMode = BlendMode.SrcAtop
        )
    }
}

private fun List<Int>.toDrawableAmplitudes(
    amplitudeType: AmplitudeType,
    spikes: Int,
    minHeight: Float,
    maxHeight: Float
): List<Float> {
    if (spikes <= 0) return emptyList()
    if (isEmpty()) return List(spikes) { minHeight }

    val samples = map { it.coerceAtLeast(0).toFloat() }
    val aggregate: (List<Float>) -> Float = { bucket ->
        val value = when (amplitudeType) {
            AmplitudeType.Avg -> bucket.average().toFloat()
            AmplitudeType.Max -> bucket.maxOrNull() ?: 0F
            AmplitudeType.Min -> bucket.minOrNull() ?: 0F
        }
        value.coerceIn(0F, MaxInputAmplitude)
    }

    return when {
        spikes > samples.size -> samples.fillToSize(spikes, aggregate)
        else -> samples.chunkToSize(spikes, aggregate)
    }.map { sample ->
        (sample / MaxInputAmplitude * maxHeight).coerceIn(minHeight, maxHeight)
    }
}
