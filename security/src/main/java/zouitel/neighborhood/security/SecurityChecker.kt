package zouitel.neighborhood.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object SecurityChecker {
    
    /**
     * Run all security checks
     */
    fun performSecurityChecks(context: Context): SecurityResult {
        return SecurityResult(
            isTampered = isTampered(context),
            isEmulator = isEmulator(),
            isDebuggerAttached = isDebuggerAttached(),
            isRooted = isRooted(context),
            isAdbEnabled = isAdbEnabled(context)
        )
    }
    
    // ========================================
    // TAMPER DETECTION
    // ========================================
    
    /**
     * Check if app has been tampered with
     */
    fun isTampered(context: Context): Boolean {
        return isDebuggable(context) ||
//        checkAppSignature(context) ||
        checkInstaller(context)
//               checkApkHash(context) ||
    }
    
    /**
     * Verify app signature hasn't been modified
     */
    fun checkAppSignature(context: Context): Boolean {
        return try {
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
            
            if (signatures == null || signatures.isEmpty()) {
                return true // No signature = tampered
            }
            
            // Get signature hash
            val signature = signatures[0]
            val md = MessageDigest.getInstance("SHA-256")
            val signatureHash = md.digest(signature.toByteArray())
            val hashString = signatureHash.joinToString("") { "%02x".format(it) }
            
            // Get it by running this code once in debug mode and logging the hash
            val expectedHash = "781b62fc7118763c7a159c4c207a9e859a4489e04eaccb50de4163e0b5da1351"
            println("app signature: $hashString")
//            Log.println(0, "signature", hashString)
            return hashString != expectedHash  // TAMPERED!

        } catch (e: Exception) {
            true // Error = suspicious
        }
    }
    
    /**
     * Check if installed from trusted source (Play Store, etc.)
     */
    fun checkInstaller(context: Context): Boolean {
        val validInstallers = listOf(
            "com.android.vending",        // Google Play Store
            "com.google.android.feedback", // Google Play Store
            "com.amazon.venezia",          // Amazon App Store
            "com.sec.android.app.samsungapps" // Samsung Galaxy Store
        )
        
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        
        // If installed from ADB or unknown source, flag it
        // In development, installer will be null, so only check in release builds
        return if (BuildConfig.DEBUG) {
            false // Don't check in debug builds
        } else {
            installer !in validInstallers
        }
    }
    
    /**
     * Check APK file hash (detect repacking)
     */
    fun checkApkHash(context: Context): Boolean {
        return try {
            val apkPath = context.packageCodePath
            val apkFile = File(apkPath)
            
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(apkFile).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            
            val hash = md.digest().joinToString("") { "%02x".format(it) }
            
            // Get it by running once and logging the hash
            val expectedHash = "db788c0f024a3c0630cabdfbc03118411abeff552f72549845e8977743889911"
//            println("apk hash: $hash")
            expectedHash != hash
            
        } catch (e: Exception) {
            false // Don't flag on error (might be permission issue)
        }
    }
    
    /**
     * Check if app is debuggable
     */
    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    // ========================================
    // EMULATOR DETECTION
    // ========================================
    
    /**
     * Detect if running on emulator
     */
    fun isEmulator(): Boolean {
        return checkEmulatorBuild() ||
               checkEmulatorFiles() ||
               checkEmulatorProperties() ||
               checkEmulatorOperatorName()
    }
    
    /**
     * Check build properties
     */
    fun checkEmulatorBuild(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                Build.PRODUCT == "google_sdk" ||
                Build.PRODUCT == "sdk_gphone64_arm64" ||
                Build.PRODUCT == "sdk_gphone_x86" ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu"))
    }
    
    /**
     * Check for emulator-specific files
     */
    fun checkEmulatorFiles(): Boolean {
        val emulatorFiles = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/socket/genyd",
            "/dev/socket/baseband_genyd"
        )
        
        return emulatorFiles.any { File(it).exists() }
    }
    
    /**
     * Check system properties
     */
    fun checkEmulatorProperties(): Boolean {
        val emulatorProps = mapOf(
            "ro.hardware" to arrayOf("goldfish", "ranchu"),
            "ro.kernel.qemu" to arrayOf("1"),
            "ro.product.device" to arrayOf("generic", "generic_x86", "generic_x86_64"),
            "ro.serialno" to arrayOf("unknown"),
            "ro.build.fingerprint" to arrayOf("generic")
        )
        
        return emulatorProps.any { (key, suspiciousValues) ->
            val prop = getSystemProperty(key)
            suspiciousValues.any { prop.contains(it, ignoreCase = true) }
        }
    }
    
    /**
     * Check operator name (emulators often have "Android" as operator)
     */
    fun checkEmulatorOperatorName(): Boolean {
        val operatorName = android.telephony.TelephonyManager::class.java
            .getDeclaredMethod("getNetworkOperatorName")
        
        return try {
            val operator = operatorName.invoke(null) as? String ?: ""
            operator.lowercase() == "android"
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get system property
     */
    private fun getSystemProperty(key: String): String {
        return try {
            Runtime.getRuntime()
                .exec(arrayOf("getprop", key))
                .inputStream
                .bufferedReader()
                .use { it.readText() }
                .trim()
        } catch (e: Exception) {
            ""
        }
    }
    
    // ========================================
    // DEBUGGER DETECTION
    // ========================================
    
    /**
     * Check if debugger is attached
     */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() ||
               Debug.waitingForDebugger() ||
               isBeingDebugged() ||
               isTracerAttached()
    }
    
    /**
     * Check if process is being debugged
     */
    fun isBeingDebugged(): Boolean {
        return try {
            val status = File("/proc/self/status").readText()
            val tracerPid = status.lines()
                .firstOrNull { it.startsWith("TracerPid:") }
                ?.substringAfter("TracerPid:")
                ?.trim()
                ?.toIntOrNull() ?: 0
            tracerPid != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check for tracer (ptrace)
     */
    fun isTracerAttached(): Boolean {
        return try {
            val wchan = File("/proc/self/wchan").readText().trim()
            wchan.contains("ptrace")
        } catch (e: Exception) {
            false
        }
    }
    
    // ========================================
    // ROOT DETECTION
    // ========================================
    
    /**
     * Check if device is rooted
     */
    fun isRooted(context: Context): Boolean {
        return checkSuBinary() ||
               checkSuperuserApk(context) ||
               checkRootFiles() ||
               checkRootProperties() ||
               checkMagisk()
    }
    
    /**
     * Check for su binary
     */
    fun checkSuBinary(): Boolean {
        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        return suPaths.any { File(it).exists() }
    }
    
    /**
     * Check for Superuser/SuperSU apps
     */
    fun checkSuperuserApk(context: Context): Boolean {
        val packages = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine"
        )
        
        return packages.any { isPackageInstalled(context, it) }
    }
    
    /**
     * Check for root-related files
     */
    fun checkRootFiles(): Boolean {
        val rootFiles = arrayOf(
            "/system/app/Superuser.apk",
            "/system/etc/init.d/99SuperSUDaemon",
            "/system/xbin/daemonsu",
            "/system/xbin/busybox"
        )
        
        return rootFiles.any { File(it).exists() }
    }
    
    /**
     * Check for root properties
     */
    fun checkRootProperties(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    /**
     * Check for Magisk (modern root method)
     */
    fun checkMagisk(): Boolean {
        val magiskPaths = arrayOf(
            "/sbin/.magisk",
            "/sbin/magisk",
            "/data/adb/magisk"
        )
        
        return magiskPaths.any { File(it).exists() }
    }
    
    /**
     * Check if package is installed
     */
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
                context.packageManager
                .getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ========================================
    // ADB DETECTION
    // ========================================
    
    /**
     * Check if ADB is enabled
     */
    fun isAdbEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Security check result
 */
data class SecurityResult(
    val isTampered: Boolean,
    val isEmulator: Boolean,
    val isDebuggerAttached: Boolean,
    val isRooted: Boolean,
    val isAdbEnabled: Boolean
) {
    val isSecure: Boolean
        get() = !isTampered && !isEmulator && !isDebuggerAttached && !isRooted
    
    val threatsDetected: List<String>
        get() = buildList {
            if (isTampered) add("App Tampered")
            if (isEmulator) add("Emulator Detected")
            if (isDebuggerAttached) add("Debugger Attached")
            if (isRooted) add("Rooted Device")
            if (isAdbEnabled) add("ADB Enabled")
        }
}