package zouitel.neighborhood.security.applock

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import zouitel.neighborhood.ui.SettingsScreenModel

@SuppressLint("ContextCastToActivity")
@Composable
fun LockModeHandler(settingsModel: SettingsScreenModel) {
    val activity = LocalContext.current as AppCompatActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by remember(settingsModel, settingsModel::observe).collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lockModeManager = remember(activity) { LockModeManager(activity) }

    LaunchedEffect(lifecycleOwner, settingsModel, settings.isLocked) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (settings.isLocked == true) {
                lockModeManager.showAuthentication()
            }

            lockModeManager.resultFlow.collectLatest { result ->
                when (result) {
                    is LockModeManager.AuthResult.Error -> {
                        snackbarHostState.showSnackbar(result.message)
                    }
                    LockModeManager.AuthResult.Failed -> {
                        snackbarHostState.showSnackbar("Authentication failed")
                    }
                    LockModeManager.AuthResult.NoHardware,
                    LockModeManager.AuthResult.HardwareUnavailable -> {
                        snackbarHostState.showSnackbar("Biometric authentication is unavailable")
                    }
                    LockModeManager.AuthResult.Success -> Unit
                    LockModeManager.AuthResult.NoneEnrolled -> {
                        settingsModel.update(settings.copy(isLocked = false))
                    }
                }
            }
        }
    }
}
