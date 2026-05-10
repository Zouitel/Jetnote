package zouitel.neighborhood.markdown.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

 object Dividers {

    @Composable
     fun Horizontal(
        color: Color,
        modifier: Modifier = Modifier,
        thickness: Dp = 1.dp,
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .height(thickness)
                .background(color)
        )
    }
}
