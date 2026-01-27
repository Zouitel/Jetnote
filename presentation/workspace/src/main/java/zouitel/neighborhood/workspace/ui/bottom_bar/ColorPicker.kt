package zouitel.neighborhood.workspace.ui.bottom_bar

import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ui.CommonColors
import kotlin.math.abs

data class ColorPicker(val onConfirmColor: (Int) -> Unit): Screen {
    @Composable
    override fun Content() {
        ColorPicker()
    }

    @SuppressLint("NotConstructor")
    @Composable
    private fun ColorPicker() {
        val bottomNav = LocalBottomSheetNavigator.current
        BackHandler {
            bottomNav.hide()
        }

        var selectedColor by remember { mutableStateOf(Color(0xFFE21D4D)) }
        var hue by remember { mutableFloatStateOf(0f) }
        var saturation by remember { mutableFloatStateOf(0.8f) }
        var brightness by remember { mutableFloatStateOf(0.9f) }
        var selectedTab by remember { mutableIntStateOf(0) }

        // Update selected color when HSB values change
        LaunchedEffect(hue, saturation, brightness) {
            selectedColor = hsbToColor(hue, saturation, brightness)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Color preview card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(selectedColor, RoundedCornerShape(16.dp))
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (selectedTab) {
                            0 -> {
                                Text(
                                    text = "#${selectedColor.toHexString()}",
                                    color = if (brightness > 0.5f) Color.Black else Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            1 -> {
                                val (r, g, b) = selectedColor.toRgbInt()
                                Text(
                                    text = "RGB($r, $g, $b)",
                                    color = if (brightness > 0.5f) Color.Black else Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            2 -> {
                                val (h, s, l) = colorToHsl(selectedColor)
                                Text(
                                    text = "HSL(${h.toInt()}°, ${(s * 100).toInt()}%, ${(l * 100).toInt()}%)",
                                    color = if (brightness > 0.5f) Color.Black else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Top tabs
                TopTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

                Spacer(modifier = Modifier.height(20.dp))

                // Color sliders
                ColorSliders(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onHueChanged = { hue = it },
                    onSaturationChanged = { saturation = it },
                    onBrightnessChanged = { brightness = it }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Color palettes
                ColorPalettes(
                    selectedColor = selectedColor,
                    onColorSelected = { color ->
                        val hsb = colorToHsb(color)
                        hue = hsb.first
                        saturation = hsb.second
                        brightness = hsb.third
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Confirm button
                Button(
                    onClick = {
                        onConfirmColor(selectedColor.toArgb())
                        bottomNav.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColor
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "Confirm Color",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (brightness > 0.5f) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    fun TopTabs(
        selectedTab: Int,
        onTabSelected: (Int) -> Unit
    ) {
        val tabs = listOf("HEX", "RGB", "HSL")
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = tabs.size
                    ),
                    onClick = { onTabSelected(index) },
                    selected = index == selectedTab,
                    label = {
                        Text(
                            label,
                            fontWeight = if (index == selectedTab) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = CommonColors.glaucous,
                        inactiveContentColor = Color.Gray
                    )
                )
            }
        }
    }

    @Composable
    fun ColorSliders(
        hue: Float,
        saturation: Float,
        brightness: Float,
        onHueChanged: (Float) -> Unit,
        onSaturationChanged: (Float) -> Unit,
        onBrightnessChanged: (Float) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hue slider
            ColorSliderRow(
                label = "Hue",
                value = hue,
                valueRange = 0f..360f,
                onValueChange = onHueChanged,
                gradient = Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color(0xFFFF8000),
                        Color.Yellow,
                        Color(0xFF80FF00),
                        Color.Green,
                        Color(0xFF00FF80),
                        Color.Cyan,
                        Color(0xFF0080FF),
                        Color.Blue,
                        Color(0xFF8000FF),
                        Color.Magenta,
                        Color(0xFFFF0080),
                        Color.Red
                    )
                ),
                displayValue = "${hue.toInt()}°"
            )

            // Saturation slider
            ColorSliderRow(
                label = "Saturation",
                value = saturation,
                valueRange = 0f..1f,
                onValueChange = onSaturationChanged,
                gradient = Brush.horizontalGradient(
                    listOf(
                        Color.White,
                        hsbToColor(hue, 1f, brightness)
                    )
                ),
                displayValue = "${(saturation * 100).toInt()}%"
            )

            // Brightness slider
            ColorSliderRow(
                label = "Brightness",
                value = brightness,
                valueRange = 0f..1f,
                onValueChange = onBrightnessChanged,
                gradient = Brush.horizontalGradient(
                    listOf(
                        Color.Black,
                        hsbToColor(hue, saturation, 1f)
                    )
                ),
                displayValue = "${(brightness * 100).toInt()}%"
            )
        }
    }

    @Composable
    fun ColorSliderRow(
        label: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        onValueChange: (Float) -> Unit,
        gradient: Brush,
        displayValue: String
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = displayValue,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Background gradient
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawRoundRect(
                        brush = gradient,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }

                // Custom slider
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val newValue = (change.position.x / size.width)
                                    .coerceIn(0f, 1f)
                                val mappedValue = valueRange.start +
                                        (valueRange.endInclusive - valueRange.start) * newValue
                                onValueChange(mappedValue)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width)
                                    .coerceIn(0f, 1f)
                                val mappedValue = valueRange.start +
                                        (valueRange.endInclusive - valueRange.start) * newValue
                                onValueChange(mappedValue)
                            }
                        }
                ) {
                    // Thumb indicator
                    val normalizedValue = (value - valueRange.start) /
                            (valueRange.endInclusive - valueRange.start)

                    val thumbColor = hsbToColor(
                        if (label == "Hue") value else 0f,
                        if (label == "Saturation") value else 1f,
                        if (label == "Brightness") value else 1f
                    )

                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val thumbX = size.width * normalizedValue
                        val thumbY = size.height / 2
                        val thumbRadius = 10.dp.toPx()

                        drawCircle(
                            color = thumbColor,
                            radius = thumbRadius,
                            center = Offset(thumbX, thumbY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius,
                            style = Stroke(width = 2.dp.toPx()),
                            center = Offset(thumbX, thumbY)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ColorPalettes(
        selectedColor: Color,
        onColorSelected: (Color) -> Unit
    ) {
        Column {
            // Shades colors
            ColorPaletteRow(
                title = "Shades",
                colors = generatePrimaryVariations(selectedColor),
                onColorSelected = onColorSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Complementary colors
            ColorPaletteRow(
                title = "Complementary",
                colors = generateComplementaryColors(selectedColor),
                onColorSelected = onColorSelected
            )
        }
    }

    @Composable
    fun ColorPaletteRow(
        title: String,
        colors: List<Color>,
        onColorSelected: (Color) -> Unit = {}
    ) {
        Column {
            Text(
                text = title,
                color = Color(0xFFAAAAAA),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(colors) { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    }

    // Utility functions
    fun generatePrimaryVariations(baseColor: Color): List<Color> {
        val hsb = colorToHsb(baseColor)
        return listOf(
            hsbToColor(hsb.first, hsb.second, hsb.third * 0.3f),
            hsbToColor(hsb.first, hsb.second, hsb.third * 0.5f),
            hsbToColor(hsb.first, hsb.second, hsb.third * 0.7f),
            baseColor,
            hsbToColor(hsb.first, hsb.second * 0.7f, minOf(1f, hsb.third * 1.2f)),
            hsbToColor(hsb.first, hsb.second * 0.4f, minOf(1f, hsb.third * 1.4f)),
            hsbToColor(hsb.first, hsb.second * 0.2f, minOf(1f, hsb.third * 1.6f))
        )
    }

    fun generateComplementaryColors(baseColor: Color): List<Color> {
        val hsb = colorToHsb(baseColor)
        val complementaryHue = (hsb.first + 180f) % 360f

        return listOf(
            hsbToColor(complementaryHue, hsb.second, hsb.third * 0.4f),
            hsbToColor(complementaryHue, hsb.second * 0.8f, hsb.third * 0.6f),
            hsbToColor(complementaryHue, hsb.second * 0.9f, hsb.third * 0.8f),
            hsbToColor(complementaryHue, hsb.second, hsb.third),
            hsbToColor(complementaryHue, hsb.second * 0.7f, minOf(1f, hsb.third * 1.2f)),
            hsbToColor(complementaryHue, hsb.second * 0.4f, minOf(1f, hsb.third * 1.4f)),
            hsbToColor(complementaryHue, hsb.second * 0.2f, minOf(1f, hsb.third * 1.6f))
        )
    }

    fun hsbToColor(hue: Float, saturation: Float, brightness: Float): Color {
        val h = hue % 360f
        val c = brightness * saturation
        val x = c * (1 - abs(((h / 60) % 2) - 1))
        val m = brightness - c

        val (r, g, b) = when ((h / 60).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = ((r + m) * 255).toInt().coerceIn(0, 255) / 255f,
            green = ((g + m) * 255).toInt().coerceIn(0, 255) / 255f,
            blue = ((b + m) * 255).toInt().coerceIn(0, 255) / 255f
        )
    }

    fun colorToHsb(color: Color): Triple<Float, Float, Float> {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val brightness = max
        val saturation = if (max == 0f) 0f else delta / max

        val hue = when {
            delta == 0f -> 0f
            max == r -> (((g - b) / delta) % 6) * 60f
            max == g -> ((b - r) / delta + 2) * 60f
            else -> ((r - g) / delta + 4) * 60f
        }

        return Triple(if (hue < 0) hue + 360f else hue, saturation, brightness)
    }

    fun Color.toHexString(): String {
        val red = (red * 255).toInt().coerceIn(0, 255)
        val green = (green * 255).toInt().coerceIn(0, 255)
        val blue = (blue * 255).toInt().coerceIn(0, 255)
        return String.format("%02X%02X%02X", red, green, blue)
    }

    fun Color.toRgbInt(): Triple<Int, Int, Int> {
        val red = (red * 255).toInt().coerceIn(0, 255)
        val green = (green * 255).toInt().coerceIn(0, 255)
        val blue = (blue * 255).toInt().coerceIn(0, 255)
        return Triple(red, green, blue)
    }

    fun colorToHsl(color: Color): Triple<Float, Float, Float> {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val l = (max + min) / 2

        val h = when {
            delta == 0f -> 0f
            max == r -> (((g - b) / delta) % 6) * 60
            max == g -> ((b - r) / delta + 2) * 60
            else -> ((r - g) / delta + 4) * 60
        }

        val s = when {
            delta == 0f -> 0f
            else -> delta / (1 - abs(2 * l - 1))
        }

        return Triple(if (h < 0) h + 360 else h, s, l)
    }
}