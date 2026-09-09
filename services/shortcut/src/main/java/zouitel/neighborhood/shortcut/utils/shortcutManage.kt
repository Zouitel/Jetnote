package zouitel.neighborhood.shortcut.utils

import android.content.Context
import android.content.pm.ShortcutManager

val Context.shortcutManage: ShortcutManager
    get() = getSystemService(ShortcutManager::class.java) as ShortcutManager