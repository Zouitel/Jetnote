package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.core.graphics.drawable.IconCompat
import zouitel.neighborhood.jotting.JottingActivity
import zouitel.neighborhood.ui.CommonIcons

fun quickPhoto(context: Context): ShortcutInfo {
    val intent = Intent(context, JottingActivity::class.java)
    intent.action = Intent.ACTION_VIEW
    intent.putExtra("quick_photo", true)
    return ShortcutInfo.Builder(context, "photo")
        .setShortLabel("Quick Photo")
        .setLongLabel("Quick Photo")
        .setIcon(IconCompat.createWithResource(context, CommonIcons.CAMERA).toIcon(context))
        .setIntent(intent)
        .build()
}