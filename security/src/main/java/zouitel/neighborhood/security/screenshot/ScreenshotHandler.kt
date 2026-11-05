package zouitel.neighborhood.security.screenshot

import android.view.Window
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import zouitel.neighborhood.ui.SettingsScreenModel

@Composable
fun ScreenshotHandler(window: Window, settingsModel: SettingsScreenModel) {
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    SideEffect {
        if (settings.isScreenshot) {
            window.setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(LayoutParams.FLAG_SECURE)
        }
    }
}
