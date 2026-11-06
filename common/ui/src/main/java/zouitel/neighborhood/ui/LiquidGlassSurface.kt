package zouitel.neighborhood.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

/**
 * Shared glass surface for presentation screens.
 *
 * The caller supplies a backdrop recorded from content behind this surface.
 * Keep the settings values in [Settings] so every screen can render the same
 * visual treatment.
 */
@Composable
fun LiquidGlassSurface(
    backdrop: Backdrop,
    enabled: Boolean,
    blurRadius: Float,
    refraction: Float,
    tintAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedCornerShape(28.dp) },
            effects = {
                if (enabled) {
                    blur(blurRadius.dp.toPx())
                    lens(
                        refractionHeight = refraction.dp.toPx(),
                        refractionAmount = refraction.dp.toPx()
                    )
                }
            },
            onDrawSurface = {
                drawRect(
                    surfaceColor.copy(
                        alpha = if (enabled) tintAlpha.coerceIn(0f, 0.4f) else 0.96f
                    )
                )
            }
        ),
        content = content
    )
}
