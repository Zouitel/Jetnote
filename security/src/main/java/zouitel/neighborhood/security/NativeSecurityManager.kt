package zouitel.neighborhood.security

/**
 * Optional JNI bridge. Construction may fail when the native library is absent
 * for the current ABI; callers must handle LinkageError and retain Kotlin checks.
 */
class NativeSecurityManager {
    init {
        System.loadLibrary("native-lib")
    }

    external fun googleClientId(): String
    external fun isEmulatorNative(): Boolean
    external fun isDebuggerAttachedNative(): Boolean
    external fun isRootedNative(): Boolean
}
