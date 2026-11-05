package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Color
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.shortcut.utils.tintedShortcutIcon
import zouitel.neighborhood.ui.CommonIcons

fun quickRecord(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra("quick_record", true)
    }
    val icon = tintedShortcutIcon(context, CommonIcons.MIC_18, Color.rgb(255, 32, 82))
    return ShortcutInfo.Builder(context, "record")
        .setShortLabel(context.getString(R.string.recorder))
        .setLongLabel(context.getString(R.string.recorder))
        .setIcon(icon)
        .setIntent(intent)
        .build()
}
