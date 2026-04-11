package zouitel.neighborhood.workspace.ui.features.document

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.ViewCompat
import kotlin.math.abs

/**
 * A plain Android [View] that:
 *   • draws a single MuPDF-rendered [Bitmap] page
 *   • supports pinch-to-zoom + pan via native gesture detectors
 *   • lets single-finger vertical swipes propagate to the parent RecyclerView
 *
 * This is the same approach used in production document viewers (ONLYOFFICE,
 * MuPDF viewer, etc.) — keep scroll in the RecyclerView, keep zoom in the View.
 */
@SuppressLint("ClickableViewAccessibility")
class DocumentPageView(context: Context) : View(context) {

    // ── State ──────────────────────────────────────────────────────────────
    private var bitmap: Bitmap? = null
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // The transform applied when drawing the bitmap
    private val drawMatrix = Matrix()

    // Current zoom level and translation
    private var scaleFactor = 1f
    private var transX      = 0f
    private var transY      = 0f

    // Initial translation to center the page horizontally
    private var baseTransX  = 0f
    private var baseTransY  = 0f

    private val minScale = 1f
    private val maxScale = 5f

    private var isFirstLayout = true
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // ── Gesture detectors ──────────────────────────────────────────────────

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val newScale = (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale)
                val ds       = newScale / scaleFactor
                scaleFactor  = newScale

                // Scale around the focal point
                transX = detector.focusX - ds * (detector.focusX - transX)
                transY = detector.focusY - ds * (detector.focusY - transY)

                clampTranslation()
                invalidate()
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                dX: Float,
                dY: Float
            ): Boolean {
                // Only pan when zoomed in; otherwise let RecyclerView scroll
                if (scaleFactor <= minScale + 0.01f) return false
                transX -= dX
                transY -= dY
                clampTranslation()
                invalidate()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (scaleFactor > minScale + 0.01f) {
                    resetZoom()
                } else {
                    val targetScale = 2.5f
                    val ds          = targetScale / scaleFactor
                    scaleFactor     = targetScale
                    transX          = e.x - ds * (e.x - transX)
                    transY          = e.y - ds * (e.y - transY)
                    clampTranslation()
                }
                invalidate()
                return true
            }
        }
    )

    // ── Public API ─────────────────────────────────────────────────────────

    fun setBitmap(bmp: Bitmap?) {
        bitmap = bmp
        isFirstLayout = true
        resetZoom()
        requestLayout()
        invalidate()
    }

    fun resetZoom() {
        scaleFactor = minScale
        transX      = baseTransX
        transY      = baseTransY
        invalidate()
    }

    // ── Touch ──────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        // Always pass to scale detector
        scaleDetector.onTouchEvent(event)

        // When zoomed in, handle pan ourselves and block parent scroll
        if (scaleFactor > minScale + 0.01f || pointerCount >= 2) {
            parent?.requestDisallowInterceptTouchEvent(true)
            gestureDetector.onTouchEvent(event)
            return true
        }

        // At base scale: let the RecyclerView handle vertical scrolling.
        // Only intercept clearly horizontal swipes (rare, but polite to handle).
        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            val dx = abs(event.x - (event.historySize.let { if (it > 0) event.getHistoricalX(0) else event.x }))
            val dy = abs(event.y - (event.historySize.let { if (it > 0) event.getHistoricalY(0) else event.y }))
            if (dx > dy && dx > touchSlop) {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        }

        gestureDetector.onTouchEvent(event)
        return true
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        updateBaseTranslation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        drawMatrix.reset()
        drawMatrix.postScale(scaleFactor, scaleFactor)
        drawMatrix.postTranslate(transX, transY)

        canvas.drawBitmap(bmp, drawMatrix, bitmapPaint)
    }

    // Compute intrinsic height based on the page aspect ratio
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val bmp = bitmap
        if (bmp != null) {
            val w      = MeasureSpec.getSize(widthMeasureSpec)
            val aspect = bmp.height.toFloat() / bmp.width.toFloat()
            val h      = (w * aspect).toInt()
            setMeasuredDimension(w, h)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun updateBaseTranslation() {
        val bmp = bitmap ?: return
        // Center-fit the bitmap in the view at scale 1
        val scaledW = bmp.width  * scaleFactor
        val scaledH = bmp.height * scaleFactor
        baseTransX = ((width  - scaledW) / 2f).coerceAtLeast(0f)
        baseTransY = ((height - scaledH) / 2f).coerceAtLeast(0f)
        if (isFirstLayout) {
            transX = baseTransX
            transY = baseTransY
            isFirstLayout = false
        }
    }

    private fun clampTranslation() {
        val bmp = bitmap ?: return
        val scaledW = bmp.width  * scaleFactor
        val scaledH = bmp.height * scaleFactor
        val minX = (width  - scaledW).coerceAtMost(0f)
        val minY = (height - scaledH).coerceAtMost(0f)
        transX = transX.coerceIn(minX, 0f.coerceAtLeast(baseTransX))
        transY = transY.coerceIn(minY, 0f)
    }
}