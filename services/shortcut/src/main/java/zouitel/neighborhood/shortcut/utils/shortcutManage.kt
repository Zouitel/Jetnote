package zouitel.neighborhood.shortcut.utils

import android.content.Context
import android.content.pm.ShortcutManager

/**
 * Returns the platform shortcut manager when dynamic shortcuts are supported.
 * The app's minimum SDK currently supports this API, but the service can still
 * be unavailable on unusual or restricted devices.
 */
val Context.shortcutManage: ShortcutManager?
    get() = getSystemService(ShortcutManager::class.java)
