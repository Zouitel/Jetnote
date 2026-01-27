package zouitel.neighborhood.ui

import androidx.compose.foundation.isSystemInDarkTheme
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
    val isDarkMode = isSystemInDarkTheme()
    val lines = text.lines()
    val contentColor = LocalContentColor.current

    val annotatedString = buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val style = when {
                index < graduatedLines -> {
                    val ratio = index.toFloat() / graduatedLines.toFloat()
                    val targetColor = if (isDarkMode) {
                        // In dark mode, fade to transparent/dimmed
                        contentColor.copy(alpha = 1f - ratio)
                    } else {
                        // In light mode, fade to very light/transparent
                        contentColor.copy(alpha = 1f - ratio)
                    }
                    normalTextStyle.copy(color = targetColor).toSpanStyle()
                }
                else -> {
                    // Completely invisible lines
                    normalTextStyle.copy(color = Color.Transparent).toSpanStyle()
                }
            }

            withStyle(style) {
                append(line)
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }

    SelectionContainer {
        Text(text = annotatedString)
    }
}