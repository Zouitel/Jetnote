package zouitel.neighborhood.shortcut.shorts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.core.graphics.drawable.IconCompat
import zouitel.neighborhood.jotting.QuickActivity
import zouitel.neighborhood.ui.CommonIcons

fun captureVideo(context: Context): ShortcutInfo {
    val intent = Intent(context, QuickActivity::class.java)
    intent.action = Intent.ACTION_VIEW
    intent.putExtra("quick_video", true)
    return ShortcutInfo.Builder(context, "capture")
        .setShortLabel("Capture Video")
        .setLongLabel("Capture Video")
        .setIcon(IconCompat.createWithResource(context, CommonIcons.VIDEO_CAMERA).toIcon(context))
        .setIntent(intent)
        .build()
}