package zouitel.neighborhood.shortcut.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.createBitmap

/**
 * Renders a drawable with its tint baked into the bitmap. ShortcutInfo does
 * not accept an Icon carrying a separate runtime tint.
 */
internal fun tintedShortcutIcon(
    context: Context,
    drawableResId: Int,
    color: Int,
    sizeDp: Int = 90
): Icon {
    require(sizeDp in 24..256) { "Shortcut icon size must be between 24 and 256 dp." }

    val drawable = requireNotNull(ContextCompat.getDrawable(context, drawableResId)) {
        "Shortcut icon drawable resource $drawableResId could not be loaded."
    }.mutate()

    val sizePx = (sizeDp * context.resources.displayMetrics.density)
        .toInt()
        .coerceAtLeast(1)
    val inset = (sizePx * 0.18f).toInt()

    DrawableCompat.setTint(drawable, color)
    drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)

    val bitmap = createBitmap(sizePx, sizePx)
    Canvas(bitmap).also(drawable::draw)

    return IconCompat.createWithAdaptiveBitmap(bitmap).toIcon(context)
}
