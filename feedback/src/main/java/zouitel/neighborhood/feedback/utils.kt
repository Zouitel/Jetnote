package zouitel.neighborhood.feedback

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.ui.graphics.Color

@SuppressLint("HardwareIds")
internal fun includeLog(): String {
    return buildString {
        appendLine("---- Device Information ----")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Model: ${Build.MODEL}")
        appendLine("Product: ${Build.PRODUCT}")
        appendLine("Device: ${Build.DEVICE}")
        appendLine("Board: ${Build.BOARD}")
        appendLine("Hardware: ${Build.HARDWARE}")
        appendLine("Fingerprint: ${Build.FINGERPRINT}")
        appendLine("---- Software Information ----")
        appendLine("OS Version: ${Build.VERSION.RELEASE}")
        appendLine("API Level: ${Build.VERSION.SDK_INT}")
        appendLine("---- Hardware Information ----")
        appendLine("CPU ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        appendLine("ID: ${Build.ID}")
        appendLine("BOOTLOADER: ${Build.BOOTLOADER}")
        appendLine("DISPLAY: ${Build.DISPLAY}")
        appendLine("HOST: ${Build.HOST}")
        appendLine("SUPPORTED_32_BIT_ABIS: ${Build.SUPPORTED_32_BIT_ABIS}")
        appendLine("SUPPORTED_64_BIT_ABIS: ${Build.SUPPORTED_64_BIT_ABIS}")
        appendLine("TAGS: ${Build.TAGS}")
        appendLine("TIME: ${Build.TIME}")
        appendLine("TYPE: ${Build.TYPE}")
        appendLine("UNKNOWN: ${Build.UNKNOWN}")
        appendLine("USER: ${Build.USER}")
        appendLine("CPU_ABI: ${Build.CPU_ABI}")
        appendLine("CPU_ABI2: ${Build.CPU_ABI2}")
        appendLine("RADIO: ${Build.RADIO}")
        appendLine("SERIAL: ${Build.SERIAL}")
        appendLine("getRadioVersion: ${Build.getRadioVersion()}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appendLine("PARTITION_NAME_SYSTEM: ${Build.Partition.PARTITION_NAME_SYSTEM}")
            appendLine("ODM_SKU: ${Build.ODM_SKU}")
            appendLine("SKU: ${Build.SKU}")
            appendLine("SOC_MANUFACTURER: ${Build.SOC_MANUFACTURER}")
            appendLine("SOC_MODEL: ${Build.SOC_MODEL}")
            appendLine("getFingerprintedPartitions: ${Build.getFingerprintedPartitions()}")
        }
    }
}

internal val defaultLabels = arrayOf(
    Label("Bug", Color.Red),
    Label("Feature", Color.Cyan),
    Label("Invalid", Color.Yellow),
    Label("Question", Color.Green),
    Label("Security", Color.Red)
)

class Label(val name: String, val color: Color)

val logsTitle = "\uD83D\uDC1E Android System Logs (Logcat) in a Feedback Report"
val logsDescription = """Android system logs are a time-stamped stream of messages from the operating system, the kernel.
    | When a user encounters an issue (like an app crash, freeze, or unexpected behavior), the logs capture the events leading up to and including the problem, providing developers with the context needed for diagnosis.""".trimMargin()