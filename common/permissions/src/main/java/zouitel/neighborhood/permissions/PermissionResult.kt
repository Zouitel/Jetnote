package zouitel.neighborhood.permissions

sealed class PermissionResult {
    object Granted : PermissionResult()
    object Denied : PermissionResult()
    object PermanentlyDenied : PermissionResult()
    data class Error(val message: String) : PermissionResult()
}