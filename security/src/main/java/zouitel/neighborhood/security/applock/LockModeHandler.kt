package zouitel.neighborhood.security.applock

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import zouitel.neighborhood.ui.SettingsScreenModel

@SuppressLint("ContextCastToActivity")
@Composable
fun LockModeHandler(settingsModel: SettingsScreenModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as AppCompatActivity

    val settings by remember(settingsModel, settingsModel::observe).collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lockModeManager = LockModeManager(activity)

    LifecycleEventEffect(Lifecycle.Event.ON_START, lifecycleOwner) {
        scope.launch(Dispatchers.Main) {
            if (settings.isLocked == true) {
                lockModeManager.showAuthentication()
            }
            lockModeManager.resultFlow.collectLatest { authResult ->
                when (authResult) {
                    is LockModeManager.AuthResult.Error -> {
                        snackbarHostState.showSnackbar(authResult.message)
                    }

                    LockModeManager.AuthResult.Failed -> {
                        snackbarHostState.showSnackbar("Authentication failed")
                    }

                    LockModeManager.AuthResult.NoHardware, LockModeManager.AuthResult.HardwareUnavailable -> {
                        snackbarHostState.showSnackbar("No biometric Hardware available")
                    }

                    LockModeManager.AuthResult.Success -> {
                    }

                    LockModeManager.AuthResult.NoneEnrolled -> {
                        // User disabled biometric authentication
                        settingsModel.update(settings.copy(isLocked = false))
                    }
                }
            }
        }
    }
}