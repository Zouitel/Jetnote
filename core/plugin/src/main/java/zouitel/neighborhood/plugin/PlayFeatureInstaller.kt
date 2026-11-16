package zouitel.neighborhood.plugin

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Installs optional feature code through Google Play.
 *
 * Large model assets are deliberately not part of this installer; they are
 * downloaded separately by the feature after the user chooses a model.
 */
class PlayFeatureInstaller(context: Context) {

    private val manager: SplitInstallManager =
        SplitInstallManagerFactory.create(context.applicationContext)

    fun isInstalled(moduleName: String): Boolean =
        moduleName in manager.installedModules

    suspend fun install(moduleName: String): Int {
        require(moduleName.isNotBlank()) { "Module name must not be blank." }
        if (isInstalled(moduleName)) return INSTALLED_SESSION

        val request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()

        return manager.startInstall(request).await()
    }

    fun observeInstall(moduleName: String): Flow<FeatureInstallState> = callbackFlow {
        if (isInstalled(moduleName)) {
            trySend(FeatureInstallState.Installed)
            close()
            return@callbackFlow
        }

        val listener = com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener { state ->
            if (moduleName !in state.moduleNames()) return@SplitInstallStateUpdatedListener

            val mapped = when (state.status()) {
                SplitInstallSessionStatus.PENDING ->
                    FeatureInstallState.Pending(state.sessionId())
                SplitInstallSessionStatus.DOWNLOADING ->
                    FeatureInstallState.Downloading(
                        state.sessionId(),
                        state.bytesDownloaded(),
                        state.totalBytesToDownload()
                    )
                SplitInstallSessionStatus.INSTALLING,
                SplitInstallSessionStatus.DOWNLOADED ->
                    FeatureInstallState.Installing(state.sessionId())
                SplitInstallSessionStatus.INSTALLED ->
                    FeatureInstallState.Installed
                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION ->
                    FeatureInstallState.AwaitingUserConfirmation(state.sessionId())
                SplitInstallSessionStatus.FAILED ->
                    FeatureInstallState.Failed(state.sessionId(), state.errorCode())
                SplitInstallSessionStatus.CANCELED ->
                    FeatureInstallState.Cancelled(state.sessionId())
                else -> null
            }

            if (mapped != null) {
                trySend(mapped)
                if (mapped is FeatureInstallState.Installed ||
                    mapped is FeatureInstallState.Failed ||
                    mapped is FeatureInstallState.Cancelled
                ) close()
            }
        }

        manager.registerListener(listener)
        awaitClose { manager.unregisterListener(listener) }
    }

    suspend fun cancel(sessionId: Int) {
        if (sessionId != INSTALLED_SESSION) manager.cancelInstall(sessionId).await()
    }

    suspend fun uninstall(moduleName: String) {
        if (isInstalled(moduleName)) {
            manager.deferredUninstall(listOf(moduleName)).await()
        }
    }

    private companion object {
        const val INSTALLED_SESSION = -1
    }
}
