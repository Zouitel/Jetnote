package zouitel.neighborhood.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun CommonDimmedText(
    text: String,
    normalTextStyle: TextStyle = TextStyle.Default,
    graduatedLines: Int = 5
) {
    val lines = text.lines()
    val contentColor = LocalContentColor.current
    val baseSpanStyle = normalTextStyle.toSpanStyle().let { base ->
        if (base.color == Color.Unspecified) base.copy(color = contentColor) else base
    }

    val annotatedString = buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val spanStyle = when {
                index < graduatedLines -> {
                    val alpha = 1f - (index.toFloat() / graduatedLines.toFloat())
                    baseSpanStyle.copy(color = baseSpanStyle.color.copy(alpha = alpha))
                }
                else -> baseSpanStyle.copy(color = Color.Transparent)
            }

            withStyle(spanStyle) { append(line) }

            if (index < lines.size - 1) append("\n")
        }
    }

    SelectionContainer {
        Text(text = annotatedString)
    }
}