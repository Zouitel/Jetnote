package zouitel.neighborhood.security.detection

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import zouitel.neighborhood.security.NativeSecurityManager
import zouitel.neighborhood.security.SecurityChecker
import zouitel.neighborhood.security.SecurityResult

class SecurityHandler(private val context: Context) {
    
    private val _securityState = MutableStateFlow<SecurityState>(SecurityState.Checking)
    val securityState = _securityState.asStateFlow()
    
    private val nativeSecurity = NativeSecurityManager()
    
    /**
     * Perform all security checks
     */
    suspend fun checkSecurity() {
        _securityState.value = SecurityState.Checking
        
        try {
            // Kotlin checks
            val kotlinResult = SecurityChecker.performSecurityChecks(context)
            
            // Native checks (harder to bypass)
            val isEmulatorNative = nativeSecurity.isEmulatorNative()
            val isDebuggerNative = nativeSecurity.isDebuggerAttachedNative()
            val isRootedNative = nativeSecurity.isRootedNative()
            
            // Combine results
            val finalResult = SecurityResult(
                isTampered = kotlinResult.isTampered,
                isEmulator = kotlinResult.isEmulator || isEmulatorNative,
                isDebuggerAttached = kotlinResult.isDebuggerAttached || isDebuggerNative,
                isRooted = kotlinResult.isRooted || isRootedNative,
                isAdbEnabled = kotlinResult.isAdbEnabled
            )
            
            Log.d("SecurityHandler", "Security check result: $finalResult")
            
            if (finalResult.isSecure) {
                _securityState.value = SecurityState.Secure
            } else {
                _securityState.value = SecurityState.Threat(finalResult)
            }
            
        } catch (e: Exception) {
            Log.e("SecurityHandler", "Security check failed", e)
            _securityState.value = SecurityState.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class SecurityState {
    object Checking : SecurityState()
    object Secure : SecurityState()
    data class Threat(val result: SecurityResult) : SecurityState()
    data class Error(val message: String) : SecurityState()
}