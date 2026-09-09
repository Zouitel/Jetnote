package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Color
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.shortcut.utils.tintedShortcutIcon
import zouitel.neighborhood.ui.CommonIcons

fun quickNote(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java)
    intent.action = Intent.ACTION_VIEW
    intent.putExtra("quick_note", true)

    val icon = tintedShortcutIcon(context, CommonIcons.NOTE_STICKY_18, Color.rgb(0, 255, 127))

    return ShortcutInfo.Builder(context, "quick")
        .setShortLabel(context.getString(R.string.jotting))
        .setLongLabel(context.getString(R.string.jotting))
        .setIcon(icon)
        .setIntent(intent)
        .build()
}