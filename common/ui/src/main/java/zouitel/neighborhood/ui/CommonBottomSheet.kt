package zouitel.neighborhood.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CommonBottomSheet(background: Int?, content: @Composable () -> Unit) {
    Surface(
        color = background?.let { Color(it) } ?: MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.background(background?.let { Color.Black.copy(alpha = .7f) }
                ?: Color.Transparent),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .padding(start = 155.dp, end = 155.dp, top = 12.dp)
                    .clip(ShapeDefaults.Small)
                    .alpha(.5f),
                thickness = 5.dp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            content.invoke()

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}