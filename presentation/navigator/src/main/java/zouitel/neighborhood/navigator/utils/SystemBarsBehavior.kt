package zouitel.neighborhood.navigator.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import zouitel.neighborhood.ui.SettingsScreenModel

@SuppressLint("WrongConstant")
@Composable
fun SystemBarsBehavior(settingsModel: SettingsScreenModel) {
    val context = LocalContext.current
    val view = LocalView.current

    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    val controller = WindowCompat.getInsetsController((context as Activity).window, view)

    LaunchedEffect(settings) {
        controller.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && settings.isZenMode ?: false) {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                show(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            }
        }
    }
}