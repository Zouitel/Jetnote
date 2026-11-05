package zouitel.neighborhood.security.detection

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import zouitel.neighborhood.security.NativeSecurityManager
import zouitel.neighborhood.security.SecurityChecker
import zouitel.neighborhood.security.SecurityResult

class SecurityHandler(private val context: Context) {

    private val _securityState = MutableStateFlow<SecurityState>(SecurityState.Checking)
    val securityState = _securityState.asStateFlow()

    suspend fun checkSecurity() = withContext(Dispatchers.IO) {
        _securityState.value = SecurityState.Checking
        try {
            val local = SecurityChecker.performSecurityChecks(context)
            val native = runCatching {
                NativeSecurityManager().let {
                    NativeSignals(
                        emulator = it.isEmulatorNative(),
                        debugger = it.isDebuggerAttachedNative(),
                        rooted = it.isRootedNative()
                    )
                }
            }

            val result = if (native.isSuccess) {
                val signals = native.getOrThrow()
                SecurityResult(
                    isTampered = local.isTampered,
                    isEmulator = local.isEmulator || signals.emulator,
                    isDebuggerAttached = local.isDebuggerAttached || signals.debugger,
                    isRooted = local.isRooted || signals.rooted,
                    isAdbEnabled = local.isAdbEnabled,
                    nativeChecksAvailable = true
                )
            } else {
                Log.w("SecurityHandler", "Native security checks unavailable", native.exceptionOrNull())
                local.copy(nativeChecksAvailable = false)
            }

            _securityState.value = if (result.isSecure) {
                SecurityState.Secure
            } else {
                SecurityState.Threat(result)
            }
        } catch (error: Exception) {
            Log.e("SecurityHandler", "Security checks failed", error)
            _securityState.value = SecurityState.Error("Unable to complete security checks")
        }
    }

    private data class NativeSignals(
        val emulator: Boolean,
        val debugger: Boolean,
        val rooted: Boolean
    )
}

sealed class SecurityState {
    data object Checking : SecurityState()
    data object Secure : SecurityState()
    data class Threat(val result: SecurityResult) : SecurityState()
    data class Error(val message: String) : SecurityState()
}
