package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Color
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.shortcut.utils.tintedShortcutIcon
import zouitel.neighborhood.ui.CommonIcons

fun quickPhoto(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java)
    intent.action = Intent.ACTION_VIEW
    intent.putExtra("quick_photo", true)

    val icon = tintedShortcutIcon(context, CommonIcons.CAMERA, Color.rgb(0, 127, 255))

    return ShortcutInfo.Builder(context, "photo")
        .setShortLabel(context.getString(R.string.take_photo))
        .setLongLabel(context.getString(R.string.take_photo))
        .setIcon(icon)
        .setIntent(intent)
        .build()
}