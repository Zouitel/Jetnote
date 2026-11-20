package zouitel.neighborhood.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle

@Composable
fun MainTheme(
    settingsModel: SettingsScreenModel,
    content: @Composable () -> Unit
) {
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()

    val isDarkTheme = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    val fontFamily = when (settings.fontFamily) {
        "sans_serif" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }
    val fontScale = when (settings.fontSize) {
        "small" -> 0.9f
        "large" -> 1.1f
        else -> 1f
    }
    val typography = MaterialTheme.typography.withFontFamily(fontFamily).scaled(fontScale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

private fun Typography.withFontFamily(fontFamily: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily)
)

private fun Typography.scaled(scale: Float) = copy(
    displayLarge = displayLarge.scaleText(scale),
    displayMedium = displayMedium.scaleText(scale),
    displaySmall = displaySmall.scaleText(scale),
    headlineLarge = headlineLarge.scaleText(scale),
    headlineMedium = headlineMedium.scaleText(scale),
    headlineSmall = headlineSmall.scaleText(scale),
    titleLarge = titleLarge.scaleText(scale),
    titleMedium = titleMedium.scaleText(scale),
    titleSmall = titleSmall.scaleText(scale),
    bodyLarge = bodyLarge.scaleText(scale),
    bodyMedium = bodyMedium.scaleText(scale),
    bodySmall = bodySmall.scaleText(scale),
    labelLarge = labelLarge.scaleText(scale),
    labelMedium = labelMedium.scaleText(scale),
    labelSmall = labelSmall.scaleText(scale)
)

private fun TextStyle.scaleText(scale: Float) = copy(
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale
)
