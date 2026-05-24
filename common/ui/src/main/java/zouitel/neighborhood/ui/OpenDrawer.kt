package zouitel.neighborhood.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenDrawer(
    settingsModel: SettingsScreenModel,
    drawerState: DrawerState,
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    IconButton({
        scope.launch {
            SoundEffect().performSoundEffect(context, CommonConstants.KEY_CLICK, settings.isSound)
            drawerState.open()
        }
    }) {
        Icon(
            painterResource(CommonIcons.MENU_BURGER),
            null
        )
    }
}