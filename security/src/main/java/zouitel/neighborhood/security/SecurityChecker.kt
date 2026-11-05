package zouitel.neighborhood.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.File
import java.security.MessageDigest

/**
 * Best-effort local risk signals. These checks are defense-in-depth only;
 * backend authorization must never depend on their results.
 */
object SecurityChecker {

    // SHA-256 of the expected production signing certificate (DER bytes).
    private const val RELEASE_CERTIFICATE_SHA256 =
        "781b62fc7118763c7a159c4c207a9e859a4489e04eaccb50de4163e0b5da1351"

    fun performSecurityChecks(context: Context): SecurityResult = SecurityResult(
        isTampered = isTampered(context),
        isEmulator = isEmulator(),
        isDebuggerAttached = isDebuggerAttached(),
        isRooted = isRooted(context),
        isAdbEnabled = isAdbEnabled(context)
    )

    /**
     * Do not use installer identity as proof of APK integrity: sideloading and
     * alternative stores are legitimate distribution channels.
     */
    fun isTampered(context: Context): Boolean {
        if (isDebuggable(context)) return true
        return !BuildConfig.DEBUG && checkAppSignature(context)
    }

    fun checkAppSignature(context: Context): Boolean = try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        val signer = signatures?.firstOrNull() ?: return true
        val actual = MessageDigest.getInstance("SHA-256")
            .digest(signer.toByteArray())
            .joinToString("") { "%02x".format(it) }

        !actual.equals(RELEASE_CERTIFICATE_SHA256, ignoreCase = true)
    } catch (_: Exception) {
        true
    }

    /**
     * Distribution-source signal only. A null/unknown installer is not proof
     * that an APK has been modified.
     */
    fun checkInstaller(context: Context): Boolean {
        if (BuildConfig.DEBUG) return false
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            installer !in TRUSTED_INSTALLERS
        } catch (_: Exception) {
            false
        }
    }

    fun checkApkHash(context: Context): Boolean = false

    fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun isEmulator(): Boolean =
        checkEmulatorBuild() || checkEmulatorFiles() || checkEmulatorProperties()

    fun checkEmulatorBuild(): Boolean =
        Build.FINGERPRINT.startsWith("generic", ignoreCase = true) ||
            Build.FINGERPRINT.startsWith("unknown", ignoreCase = true) ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for x86", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            (Build.BRAND.startsWith("generic", ignoreCase = true) &&
                Build.DEVICE.startsWith("generic", ignoreCase = true)) ||
            Build.PRODUCT in setOf("google_sdk", "sdk_gphone64_arm64", "sdk_gphone_x86") ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true)

    fun checkEmulatorFiles(): Boolean = EMULATOR_FILES.any { File(it).exists() }

    fun checkEmulatorProperties(): Boolean {
        val emulatorProps = mapOf(
            "ro.hardware" to listOf("goldfish", "ranchu"),
            "ro.kernel.qemu" to listOf("1"),
            "ro.product.device" to listOf("generic", "generic_x86", "generic_x86_64"),
            "ro.build.fingerprint" to listOf("generic")
        )
        return emulatorProps.any { (key, values) ->
            val property = getSystemProperty(key)
            values.any { property.contains(it, ignoreCase = true) }
        }
    }

    /** Retained for source compatibility; operator-name heuristics are unreliable. */
    fun checkEmulatorOperatorName(): Boolean = false

    private fun getSystemProperty(key: String): String = try {
        Runtime.getRuntime().exec(arrayOf("getprop", key)).let { process ->
            process.inputStream.bufferedReader().use { it.readText().trim() }
                .also { process.destroy() }
        }
    } catch (_: Exception) {
        ""
    }

    fun isDebuggerAttached(): Boolean =
        Debug.isDebuggerConnected() || Debug.waitingForDebugger() ||
            isBeingDebugged() || isTracerAttached()

    fun isBeingDebugged(): Boolean = try {
        File("/proc/self/status").useLines { lines ->
            lines.firstOrNull { it.startsWith("TracerPid:") }
                ?.substringAfter("TracerPid:")?.trim()?.toIntOrNull()?.let { it != 0 }
                ?: false
        }
    } catch (_: Exception) {
        false
    }

    fun isTracerAttached(): Boolean = try {
        File("/proc/self/wchan").readText().trim().contains("ptrace", ignoreCase = true)
    } catch (_: Exception) {
        false
    }

    fun isRooted(context: Context): Boolean =
        checkSuBinary() || checkSuperuserApk(context) || checkRootFiles() ||
            checkRootProperties() || checkMagisk()

    fun checkSuBinary(): Boolean = SU_PATHS.any { File(it).exists() }

    fun checkSuperuserApk(context: Context): Boolean = ROOT_PACKAGES.any { packageName ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun checkRootFiles(): Boolean = ROOT_FILES.any { File(it).exists() }

    fun checkRootProperties(): Boolean = Build.TAGS?.contains("test-keys") == true

    fun checkMagisk(): Boolean = MAGISK_PATHS.any { File(it).exists() }

    fun isAdbEnabled(context: Context): Boolean = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    } catch (_: Exception) {
        false
    }

    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.google.android.feedback",
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps"
    )

    private val EMULATOR_FILES = arrayOf(
        "/dev/socket/qemud", "/dev/qemu_pipe",
        "/system/lib/libc_malloc_debug_qemu.so", "/sys/qemu_trace",
        "/system/bin/qemu-props", "/dev/socket/genyd", "/dev/socket/baseband_genyd"
    )

    private val SU_PATHS = arrayOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su",
        "/system/bin/.ext/.su", "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
    )

    private val ROOT_PACKAGES = arrayOf(
        "com.noshufou.android.su", "com.thirdparty.superuser", "eu.chainfire.supersu",
        "com.koushikdutta.superuser", "com.zachspong.temprootremovejb",
        "com.ramdroid.appquarantine"
    )

    private val ROOT_FILES = arrayOf(
        "/system/app/Superuser.apk", "/system/etc/init.d/99SuperSUDaemon",
        "/system/xbin/daemonsu", "/system/xbin/busybox"
    )

    private val MAGISK_PATHS = arrayOf("/sbin/.magisk", "/sbin/magisk", "/data/adb/magisk")
}

data class SecurityResult(
    val isTampered: Boolean,
    val isEmulator: Boolean,
    val isDebuggerAttached: Boolean,
    val isRooted: Boolean,
    val isAdbEnabled: Boolean,
    val nativeChecksAvailable: Boolean = true
) {
    /** ADB and missing native checks are surfaced as risks, not silently ignored. */
    val isSecure: Boolean
        get() = !isTampered && !isEmulator && !isDebuggerAttached &&
            !isRooted && !isAdbEnabled && nativeChecksAvailable

    val threatsDetected: List<String>
        get() = buildList {
            if (isTampered) add("App integrity check failed")
            if (isEmulator) add("Emulator Detected")
            if (isDebuggerAttached) add("Debugger Attached")
            if (isRooted) add("Rooted Device")
            if (isAdbEnabled) add("ADB Enabled")
            if (!nativeChecksAvailable) add("Native security checks unavailable")
        }
}
