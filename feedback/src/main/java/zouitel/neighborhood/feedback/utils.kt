package zouitel.neighborhood.feedback

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import zouitel.neighborhood.logic.R

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
        appendLine("TAGS_36: ${Build.TAGS}")
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

@Composable
fun defaultLabels() = arrayOf(
    Label(stringResource(R.string.label_bug), Color.Red),
    Label(stringResource(R.string.label_feature), Color.Cyan),
    Label(stringResource(R.string.label_invalid), Color.Yellow),
    Label(stringResource(R.string.label_question), Color.Green),
    Label(stringResource(R.string.label_security), Color.Red)
)

class Label(val name: String, val color: Color)