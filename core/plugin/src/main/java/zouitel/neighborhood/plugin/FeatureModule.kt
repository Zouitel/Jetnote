package zouitel.neighborhood.plugin

/**
 * Stable contract implemented by optional Play-delivered feature modules.
 *
 * Keep this API in the base application dependency graph. Feature implementations
 * must not be referenced directly by base-app source code.
 */
interface FeatureModule {
    val id: String
    val displayName: String
}

/**
 * Runtime state for a Play Feature Delivery request.
 */
sealed interface FeatureInstallState {
    data object NotInstalled : FeatureInstallState
    data class Pending(val sessionId: Int) : FeatureInstallState
    data class Downloading(
        val sessionId: Int,
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long
    ) : FeatureInstallState
    data class Installing(val sessionId: Int) : FeatureInstallState
    data object Installed : FeatureInstallState
    data class AwaitingUserConfirmation(val sessionId: Int) : FeatureInstallState
    data class Failed(val sessionId: Int, val errorCode: Int) : FeatureInstallState
    data class Cancelled(val sessionId: Int) : FeatureInstallState
}
