package zouitel.neighborhood.navigator.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ui.CommonIcons.TINT_SLASH
import zouitel.neighborhood.workspace.R
import zouitel.neighborhood.workspace.ui.bottom_bar.ColorPicker
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
internal fun ColorsRow(colors: Array<Int>, onAction: (Int) -> Unit) {
    val currentColor = remember { mutableStateOf(Color.White.toArgb()) }
    val bottomNav = LocalBottomSheetNavigator.current
    val selectedIndex = remember { mutableIntStateOf(-1) }
    val defaultBackgroundColor = MaterialTheme.colorScheme.surface.toArgb()

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .imePadding()
            .navigationBarsPadding()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            itemsIndexed(items = colors) { index, color ->
                val distance = abs(index - selectedIndex.intValue)
                val scale = remember { Animatable(1f) }

                LaunchedEffect(selectedIndex.intValue) {
                    if (selectedIndex.intValue != -1) {
                        when (distance) {
                            0 -> {
                                scale.animateTo(1.2f, tween(100))
                                scale.animateTo(1f, tween(100))
                            }
                            1 -> {
                                scale.animateTo(1.1f, tween(100))
                                scale.animateTo(1f, tween(100))
                            }
                            2 -> {
                                scale.animateTo(1.05f, tween(100))
                                scale.animateTo(1f, tween(100))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))
                Canvas(
                    modifier = Modifier
                        .size(27.dp)
                        .scale(scale.value)
                        .clickable {
                            currentColor.value = color
                            selectedIndex.intValue = index
                            onAction(color)
                        }
                ) {
                    drawArc(
                        color = Color(color),
                        startAngle = 1f,
                        sweepAngle = 360f,
                        useCenter = true,
                        style =
                            if (currentColor.value == color) {
                                Stroke(width = 10f, cap = StrokeCap.Round)
                            } else {
                                Fill
                            }
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            item {
                IconButton({
                    onAction(defaultBackgroundColor)
                }) {
                    Icon(
                        painter = painterResource(id = TINT_SLASH),
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            item {
                IconButton(
                    modifier = Modifier.size(38.dp),
                    onClick = {
                        bottomNav.show(ColorPicker(onAction))
                    }) {
                    AsyncImage(
                        model = R.drawable.color_wheel,
                        contentDescription = null,
                        modifier = Modifier.clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
    }
}