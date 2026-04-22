package zouitel.neighborhood.account.utils

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
@Composable
fun PhotoProfile(imageModel: Uri?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp * 2)
    ) {
        // Main circular image
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )

        // Composable for the colorful dots
        ColorfulDotRing(
            dotRingRadius = 73.dp,
            dotCount = 30,
            minDotSize = 0.dp,
            maxDotSize = 15.dp
        )
    }
}

@Composable
private fun ColorfulDotRing(
    dotRingRadius: Dp,
    dotCount: Int,
    minDotSize: Dp,
    maxDotSize: Dp
) {
    val colors = listOf(
        Color(0xFF4285F4),
        Color(0xFFDB4437),
        Color(0xFFF4B400),
        Color(0xFF0F9D58)
    )

    Canvas(modifier = Modifier.size(dotRingRadius * 2)) {
        val radiusPx = dotRingRadius.toPx()
        val minDotSizePx = minDotSize.toPx()
        val maxDotSizePx = maxDotSize.toPx()

        val angleStep = 360f / dotCount

        for (i in 0 until dotCount) {
            val angle = i * angleStep
            val x = (radiusPx * cos(Math.toRadians(angle.toDouble()))).toFloat()
            val y = (radiusPx * sin(Math.toRadians(angle.toDouble()))).toFloat()

            // Randomly pick a color for each dot
            val dotColor = colors[Random.nextInt(colors.size)]

            // Randomly determine the size of the dot within the specified range
            val randomDotSizePx = Random.nextFloat() * (maxDotSizePx - minDotSizePx) + minDotSizePx

            drawCircle(
                color = dotColor,
                radius = (randomDotSizePx / 2),
                center = this.center.plus(Offset(x, y))
            )
        }
    }
}