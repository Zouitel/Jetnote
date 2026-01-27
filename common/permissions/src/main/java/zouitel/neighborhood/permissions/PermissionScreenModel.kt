package zouitel.neighborhood.permissions

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState.Denied
import dev.icerock.moko.permissions.PermissionState.Granted
import dev.icerock.moko.permissions.PermissionState.NotGranted
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

@Stable
class PermissionScreenModel(private val controller: PermissionsController): ScreenModel {

    fun activityBind(activity: ComponentActivity): Boolean {
        return try {
            controller.bind(activity)
            true
        } catch (e: Exception) {
            println("Error binding activity: ${e.message}")
            false
        }
    }

    fun requestPermission(permission: Permission) = flow {
        try {
            when (controller.getPermissionState(permission)) {
                NotGranted -> {
                    try {
                        controller.providePermission(permission)
                        if (controller.isPermissionGranted(permission)) {
                            emit(PermissionResult.Granted)
                        } else {
                            emit(PermissionResult.Denied)
                        }
                    } catch (e: Exception) {
                        if (e.cause == null) {
                            // Permanently denied
                            emit(PermissionResult.PermanentlyDenied)
                        } else {
                            emit(PermissionResult.Error(e.message ?: "Unknown error"))
                        }
                    }
                }

                Denied -> emit(PermissionResult.PermanentlyDenied)
                Granted -> emit(PermissionResult.Granted)
                else -> emit(PermissionResult.Error("Unknown permission state"))
            }
        } catch (e: Exception) {
            emit(PermissionResult.Error(e.message ?: "Unexpected error"))
        }
    }.catch { throwable ->
        emit(PermissionResult.Error(throwable.message ?: "Flow error"))
    }

    suspend fun permissionDeniedHandle(state: SnackbarHostState) {
        val result = state.showSnackbar(
            "The permission is denied!",
            "Settings",
            false,
            SnackbarDuration.Long
        )
        when (result) {
            SnackbarResult.Dismissed -> {}
            SnackbarResult.ActionPerformed -> controller.openAppSettings()
        }
    }
}