package zouitel.neighborhood.ui.liquid_glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur

fun Modifier.commonBlur(backdrop: Backdrop, surfaceColor: Color) = this
    .border(BorderStroke(0.dp, Color.Transparent))
    .drawBackdrop(
        backdrop = backdrop,
        shape = { RectangleShape },
        highlight = null,
        shadow = null,
        effects = { blur(16.dp.toPx()) },
        onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.15f)) },
    )