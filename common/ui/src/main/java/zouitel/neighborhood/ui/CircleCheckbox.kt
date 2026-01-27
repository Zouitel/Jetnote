package zouitel.neighborhood.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedCircleCheckbox(
    checked: Boolean,
    textColor: Color,
    background: Color,
    size: Dp = 27.dp,
    onCheckedChange: (Boolean) -> Unit
) {
    val checkmarkColor by animateColorAsState(
        targetValue = if (checked) Color.Gray else Color.Transparent,
        label = "checkmark"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked)
            Color.Gray
        else
            textColor,
        label = "border"
    )

    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(
                if (checked) checkmarkColor
                else Color.Transparent,
                CircleShape
            )
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton({
            onCheckedChange(!checked)
        }) {
            Icon(
                painter = painterResource(CommonIcons.CHECK),
                contentDescription = null,
                tint = background,
                modifier = Modifier
                    .size(size * 0.6f)
                    .scale(scale)
            )
        }
    }
}