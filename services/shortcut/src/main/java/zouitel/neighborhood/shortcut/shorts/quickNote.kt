package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.core.graphics.drawable.IconCompat
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.CommonIcons

fun quickNote(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java)
    intent.action = Intent.ACTION_VIEW
    intent.putExtra("quick_note", true)
    return ShortcutInfo.Builder(context, "quick")
        .setShortLabel(context.getString(R.string.jotting))
        .setLongLabel(context.getString(R.string.jotting))
        .setIcon(IconCompat.createWithResource(context, CommonIcons.PLUS_18).toIcon(context))
        .setIntent(intent)
        .build()
}