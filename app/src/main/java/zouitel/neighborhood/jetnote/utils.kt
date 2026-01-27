package zouitel.neighborhood.jetnote

import android.annotation.SuppressLint
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import zouitel.neighborhood.ui.SettingsScreenModel
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

@Composable
@SuppressLint("WrongConstant")
internal fun HideSystemUI(window: Window, isZenMode: Boolean) {
    val settingsModel by inject<SettingsScreenModel>(SettingsScreenModel::class.java)
    val view = LocalView.current
    val controller = WindowCompat.getInsetsController(window, view)
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    var isZen = false

    LaunchedEffect(Unit) {
        isZen = settings.isZenMode ?: false
    }

    controller.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isZenMode) {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

    /*
    fun void() {
        mapOf(
            "Coffee" to "Prepare hot coffee for my self.",
            "Certification" to "Call instructor for complete details.",
            "Team Meeting" to "Planning sprint log for next product application update.",
            "Birthday Party" to "Tomorrow is my brother birthday there will be party at 7:00 pm.",
            "Vacation Tickets" to "Buy tickets for the family vacation.",
            "Appointment" to "Health check up with physician."

        ).forEach {
            viewmodel.value.addNote(
                Note(
                    uid = UUID.randomUUID().toString(),
                    title = it.key,
                    description = it.value
                )
            )
        }
    }
    */