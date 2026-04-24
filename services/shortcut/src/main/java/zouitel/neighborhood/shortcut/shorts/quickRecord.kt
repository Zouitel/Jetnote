package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.core.graphics.drawable.IconCompat
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.CommonIcons

fun quickRecord(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java)
    intent.action = Intent.ACTION_VIEW
    intent.putExtra("quick_record", true)
    return ShortcutInfo.Builder(context, "record")
        .setShortLabel(context.getString(R.string.recorder))
        .setLongLabel(context.getString(R.string.recorder))
        .setIcon(IconCompat.createWithResource(context, CommonIcons.MIC_18).toIcon(context))
        .setIntent(intent)
        .build()
}