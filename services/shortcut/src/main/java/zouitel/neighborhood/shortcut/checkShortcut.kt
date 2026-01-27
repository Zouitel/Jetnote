package zouitel.neighborhood.shortcut

import android.content.Context
import zouitel.neighborhood.logic.asShortToast
import zouitel.neighborhood.shortcut.shorts.quickRecord
import zouitel.neighborhood.shortcut.shorts.quickNote
import zouitel.neighborhood.shortcut.shorts.quickReminder
import zouitel.neighborhood.shortcut.shorts.quickPhoto

fun checkNoteActivityShortcut(context: Context) {
//    val quickPhotoShortcut = quickPhoto(context)
//    val quickRecordShortcut = quickRecord(context)
    val quickNoteShortcut = quickNote(context)
    val quickReminderShortcut = quickReminder(context)

    runCatching {
        context.shortcutManage.dynamicShortcuts =
            listOf(quickNoteShortcut, /*quickRecordShortcut,*/ /*quickPhotoShortcut,*/ quickReminderShortcut)
    }.onFailure {
        context.run {
            "something wring!".asShortToast()
        }
    }
}