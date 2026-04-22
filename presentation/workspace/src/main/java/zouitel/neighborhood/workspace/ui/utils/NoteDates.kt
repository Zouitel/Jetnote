package zouitel.neighborhood.workspace.ui.utils

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.model.Data

private fun dayIconFor(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> CommonIcons.SATURDAY
    DayOfWeek.SUNDAY -> CommonIcons.SUNDAY
    DayOfWeek.MONDAY -> CommonIcons.MONDAY
    DayOfWeek.TUESDAY -> CommonIcons.TUESDAY
    DayOfWeek.WEDNESDAY -> CommonIcons.WEDNESDAY
    DayOfWeek.THURSDAY -> CommonIcons.THURSDAY
    DayOfWeek.FRIDAY -> CommonIcons.FRIDAY
}

@Composable
internal fun NoteDates(data: Data) {
    val createdLocal = data.date.toLocalDateTime(TimeZone.currentSystemDefault())
    val editedLocal = data.lastEdit.toLocalDateTime(TimeZone.currentSystemDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateChip(
            label = stringResource(R.string.created),
            icon = dayIconFor(createdLocal.dayOfWeek),
            formattedDate = createdLocal.format(formatter),
            relativeTime = data.date.toRelativeTime(),
            accentColor = CommonColors.prime,
            modifier = Modifier.weight(1f)
        )
        DateChip(
            label = stringResource(R.string.edited),
            icon = dayIconFor(editedLocal.dayOfWeek),
            formattedDate = editedLocal.format(formatter),
            relativeTime = data.lastEdit.toRelativeTime(),
            accentColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateChip(
    label: String,
    icon: Int,
    formattedDate: String,
    relativeTime: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var showRelative by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "chipScale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (showRelative)
            accentColor.copy(alpha = 0.12f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 280),
        label = "chipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (showRelative)
            accentColor.copy(alpha = 0.35f)
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        animationSpec = tween(durationMillis = 280),
        label = "chipBorder"
    )

    val iconTint by animateColorAsState(
        targetValue = if (showRelative)
            accentColor
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 280),
        label = "iconTint"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(MaterialTheme.shapes.large)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { showRelative = !showRelative }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (showRelative)
                    accentColor.copy(alpha = 0.85f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Crossfade(
            targetState = showRelative,
            animationSpec = tween(durationMillis = 220),
            label = "dateText"
        ) { relative ->
            Text(
                text = if (relative) relativeTime else formattedDate,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (relative) 0.9f else 0.65f
                ),
            )
        }
    }
}