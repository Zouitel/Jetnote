package zouitel.neighborhood.navigator.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import zouitel.neighborhood.ui.SettingsScreenModel

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("WrongConstant")
@Composable
internal fun SystemBarsBehavior(settingsModel: SettingsScreenModel) {
    val context = LocalContext.current
    val view = LocalView.current

    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    val controller = WindowCompat.getInsetsController((context as Activity).window, view)

    LaunchedEffect(settings) {
        controller.apply {
            show(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
        }
    }
}