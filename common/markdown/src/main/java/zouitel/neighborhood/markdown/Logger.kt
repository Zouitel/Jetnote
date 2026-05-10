// Logger.kt  — replace the expect file entirely
package zouitel.neighborhood.markdown

import android.util.Log

internal fun logd(message: String, tag: String = "CascadeEditor") {
    Log.d(tag, message)
}

internal fun loge(message: String, tag: String = "CascadeEditor") {
    Log.e(tag, message)
}