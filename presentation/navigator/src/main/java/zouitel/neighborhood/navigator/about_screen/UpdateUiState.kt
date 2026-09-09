package zouitel.neighborhood.navigator.about_screen

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: AppUpdateInfo) : UpdateUiState
    data object Downloading : UpdateUiState
    data object ReadyToInstall : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

class UpdateScreenModel(context: Context) : ScreenModel {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val listener = InstallStateUpdatedListener { installState ->
        _state.value = when (installState.installStatus()) {
            InstallStatus.DOWNLOADED -> UpdateUiState.ReadyToInstall
            InstallStatus.DOWNLOADING, InstallStatus.PENDING, InstallStatus.INSTALLING -> UpdateUiState.Downloading
            InstallStatus.FAILED -> UpdateUiState.Failed("Update download failed")
            else -> _state.value
        }
    }

    init {
        appUpdateManager.registerListener(listener)
    }

    fun checkForUpdate() {
        screenModelScope.launch {
            _state.value = UpdateUiState.Checking
            _state.value = try {
                val info = appUpdateManager.appUpdateInfo.await()
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    UpdateUiState.Available(info)
                } else {
                    UpdateUiState.UpToDate
                }
            } catch (t: Throwable) {
                UpdateUiState.Failed(t.message ?: "Couldn't reach Play Store")
            }
        }
    }

    fun startUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>, flexible: Boolean = true) {
        val current = _state.value
        if (current !is UpdateUiState.Available) return
        val type = if (flexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
        if (!current.info.isUpdateTypeAllowed(type)) return
        appUpdateManager.startUpdateFlowForResult(
            current.info,
            launcher,
            AppUpdateOptions.newBuilder(type).build()
        )
    }

    fun completeUpdate() = appUpdateManager.completeUpdate()

    override fun onDispose() {
        appUpdateManager.unregisterListener(listener)
        super.onDispose()
    }
}