package zouitel.neighborhood.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(
    settingsModel: SettingsScreenModel,
    drawerState: DrawerState,
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    backdrop: Backdrop? = null,
    liquidGlassEnabled: Boolean = false,
    liquidGlassBlurRadius: Float = 18f,
    liquidGlassRefraction: Float = 12f,
    liquidGlassTint: Float = 0.12f
) {
    val glassActive = liquidGlassEnabled && backdrop != null
    val surfaceColor = MaterialTheme.colorScheme.surface
    val glassModifier = if (glassActive) {
        Modifier.drawBackdrop(
            backdrop = requireNotNull(backdrop),
            shape = { RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp) },
            effects = {
                blur(liquidGlassBlurRadius.coerceAtLeast(0f).dp.toPx())
                if (liquidGlassRefraction > 0f) {
                    lens(
                        refractionHeight = liquidGlassRefraction.dp.toPx(),
                        refractionAmount = liquidGlassRefraction.dp.toPx()
                    )
                }
            },
            onDrawSurface = {
                drawRect(
                    surfaceColor.copy(alpha = liquidGlassTint.coerceIn(0f, 0.4f))
                )
            }
        )
    } else {
        Modifier
    }

    TopAppBar(
        modifier = glassModifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (glassActive) Color.Transparent else surfaceColor,
            scrolledContainerColor = if (glassActive) Color.Transparent else surfaceColor
        ),
        navigationIcon = {
            Row {
                CommonRow(
                    Modifier.padding(start = 10.dp, end = 10.dp),
                ) {
                    OpenDrawer(settingsModel, drawerState = drawerState)
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 15.dp)
            )
        },
        scrollBehavior = scrollBehavior
    )
}
