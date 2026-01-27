package zouitel.neighborhood.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun MainTheme(
    settingsModel: SettingsScreenModel,
    content: @Composable () -> Unit
) {
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()

    val theme = when {
        isSystemInDarkTheme() -> darkColorScheme()
        settings.darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = theme, content = content)
}
