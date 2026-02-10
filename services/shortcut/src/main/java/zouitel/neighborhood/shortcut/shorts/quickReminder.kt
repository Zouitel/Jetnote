package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.core.graphics.drawable.IconCompat
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.ui.CommonIcons

fun quickReminder(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java)
    intent.action = Intent.ACTION_VIEW
    intent.putExtra("quick_reminder", true)
    return ShortcutInfo.Builder(context, "reminder")
        .setShortLabel("Quick Reminder")
        .setLongLabel("Quick Reminder")
        .setIcon(IconCompat.createWithResource(context, CommonIcons.BELL).toIcon(context))
        .setIntent(intent)
        .build()
}