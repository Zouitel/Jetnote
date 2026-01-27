package zouitel.neighborhood.search.utils

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import zouitel.neighborhood.logic.Constants.HIG
import zouitel.neighborhood.logic.Constants.LOW
import zouitel.neighborhood.logic.Constants.MED
import zouitel.neighborhood.logic.Constants.URG
import zouitel.neighborhood.workspace.model.Data

internal val DrawScope.prioritizedNotePath: (Data) -> Unit get() = {
    clipPath(
        Path().apply {
            lineTo(size.width, 0f)
            lineTo(size.width,size.height - 25.dp.toPx())
            lineTo(size.width - 25.dp.toPx(), size.height)
            lineTo(0f,size.height)
            close()
        }
    ) {

        drawRoundRect(
            color = it.background?.let { color -> Color(color) } ?: return@clipPath,
            size = size,
            cornerRadius = CornerRadius(15.dp.toPx())
        )
        drawRoundRect(
            color = Color(
                ColorUtils.blendARGB(getColorOfPriority(it.priority), 0x000000, .05f)
            ),
            topLeft = Offset(size.width - 25.dp.toPx(), size.height - 25.dp.toPx()),
            size = Size(
                25.dp.toPx() + 100f, 25.dp.toPx() + 100f
            ),
            cornerRadius = CornerRadius(15.dp.toPx())
        )
    }
}

internal val DrawScope.normalNotePath: (Data) -> Unit get() = {
    clipPath(
        Path().apply {
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
    ) {
        drawRoundRect(
            color = it.background?.let { color -> Color(color) } ?: return@clipPath,
            size = size,
            cornerRadius = CornerRadius(15.dp.toPx())
        )
    }
}

private val getColorOfPriority: (color: Int) -> Int = {
    when (it) {
        URG -> 0xffff2800.toInt()
        HIG -> 0xffffa500.toInt()
        MED -> 0xffffef00.toInt()
        LOW -> 0xff32cd32.toInt()
        else -> 0xffc0c0c0.toInt()
    }
}