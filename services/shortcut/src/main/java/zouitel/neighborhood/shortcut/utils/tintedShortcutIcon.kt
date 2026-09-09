package zouitel.neighborhood.shortcut.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.createBitmap

/**
 * Renders a vector/drawable resource with a color baked in, since ShortcutInfo
 * rejects any Icon that carries a runtime tint (icon.hasTint() must be false).
 */
internal fun tintedShortcutIcon(
    context: Context,
    drawableResId: Int,
    color: Int,
    sizeDp: Int = 90
): Icon {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()

    val drawable = ContextCompat.getDrawable(context, drawableResId)!!.mutate()
    DrawableCompat.setTint(drawable, color)

    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    // Keep a safe-zone inset (~18%) so launchers that apply an adaptive mask
    // (circle/squircle) don't clip the glyph.
    val inset = (sizePx * 0.18f).toInt()
    drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
    drawable.draw(canvas)

    return IconCompat.createWithAdaptiveBitmap(bitmap).toIcon(context)
}