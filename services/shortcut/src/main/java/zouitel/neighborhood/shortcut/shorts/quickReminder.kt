package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Color
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.shortcut.utils.tintedShortcutIcon
import zouitel.neighborhood.ui.CommonIcons

fun quickReminder(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra("quick_reminder", true)
    }
    val icon = tintedShortcutIcon(context, CommonIcons.BELL_RING_18, Color.rgb(255, 130, 67))
    return ShortcutInfo.Builder(context, "reminder")
        .setShortLabel(context.getString(R.string.reminder))
        .setLongLabel(context.getString(R.string.reminder))
        .setIcon(icon)
        .setIntent(intent)
        .build()
}
