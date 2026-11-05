package zouitel.neighborhood.shortcut.utils

import android.content.Context
import android.util.Log
import zouitel.neighborhood.shortcut.shorts.quickNote
import zouitel.neighborhood.shortcut.shorts.quickPhoto
import zouitel.neighborhood.shortcut.shorts.quickRecord
import zouitel.neighborhood.shortcut.shorts.quickReminder

private const val TAG = "JetnoteShortcuts"
private val jetnoteShortcutIds = setOf("quick", "quick-note", "record", "photo", "reminder")

/**
 * Publishes Jetnote's launcher shortcuts without deleting dynamic shortcuts
 * owned by other app features. Returns false when registration is unavailable
 * or rejected by the launcher.
 */
fun checkNoteActivityShortcut(context: Context): Boolean {
    val manager = context.shortcutManage ?: run {
        Log.w(TAG, "ShortcutManager is unavailable.")
        return false
    }

    return runCatching {
        val shortcuts = listOf(
            quickNote(context),
            quickRecord(context),
            quickPhoto(context),
            quickReminder(context)
        )
        val existing = manager.dynamicShortcuts.filterNot { it.id in jetnoteShortcutIds }
        val combined = (existing + shortcuts).distinctBy { it.id }
        val limit = manager.maxShortcutCountPerActivity
        manager.dynamicShortcuts = combined.take(limit)
        true
    }.onFailure { error ->
        Log.w(TAG, "Unable to register launcher shortcuts.", error)
    }.getOrDefault(false)
}
