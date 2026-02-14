package zouitel.neighborhood.security

/**
 * Bridge class to call native C++ security functions
 */
class NativeSecurityManager {
    // Load native library when class is first used
    init {
        System.loadLibrary("native-lib")
    }

    external fun googleClientId(): String
}