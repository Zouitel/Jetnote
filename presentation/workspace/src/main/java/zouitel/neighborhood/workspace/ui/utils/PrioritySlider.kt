package zouitel.neighborhood.workspace.ui.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.workspace.model.Priorities

@Composable
internal fun PrioritySlider(
    priority: Priorities,
    onPriorityChange: (Priorities) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(priority.ordinal.toFloat()) }
    var selectedPriority by remember { mutableStateOf(priority) }
    var sliderWidth by remember { mutableStateOf(0f) }
    val thumbColor = selectedPriority.color

    LaunchedEffect(sliderPosition) {
        selectedPriority = when (sliderPosition.toInt()) {
            1 -> Priorities.LOW
            2 -> Priorities.MED
            3 -> Priorities.HIG
            4 -> Priorities.URG
            else -> Priorities.NON
        }
        onPriorityChange(selectedPriority)
    }

    Slider(
        value = sliderPosition,
        onValueChange = { newValue ->
            sliderPosition = newValue
        },
        valueRange = 0f..4f,
        steps = 3,
        onValueChangeFinished = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp)
            .onGloballyPositioned { coordinates ->
                sliderWidth = coordinates.size.width.toFloat()
            },
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = thumbColor.copy(alpha = 0.4f),
            inactiveTrackColor = thumbColor.copy(alpha = 0.15f),
        )
    )
}