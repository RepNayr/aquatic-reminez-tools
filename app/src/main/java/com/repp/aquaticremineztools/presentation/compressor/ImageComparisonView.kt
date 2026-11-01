package com.repp.aquaticremineztools.presentation.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class ImageComparisonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmapBefore: Bitmap? = null
    private var bitmapAfter: Bitmap? = null

    // Slider position (0.0 to 1.0), initially 0.5 (50/50)
    private var sliderPosition: Float = 0.5f
    private var isDraggingSlider: Boolean = false

    // Synchronized Pan & Zoom
    private var scaleFactor: Float = 1.0f
    private var focusX: Float = 0f
    private var focusY: Float = 0f
    private var panX: Float = 0f
    private var panY: Float = 0f

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var activePointerId: Int = INVALID_POINTER_ID

    private val scaleDetector: ScaleGestureDetector

    // Paints & Styling
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#43D9E6") // Cyan accent
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val handleCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#43D9E6")
        style = Paint.Style.FILL
    }

    private val handleInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#090B0F")
        style = Paint.Style.FILL
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC151A21")
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val clipPath = Path()
    private val dstRectF = RectF()
    private val srcRect = Rect()

    companion object {
        private const val INVALID_POINTER_ID = -1
        private const val HANDLE_RADIUS = 28f
        private const val SLIDER_TOUCH_TOLERANCE = 48f
    }

    init {
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = max(1.0f, min(scaleFactor, 5.0f))
                invalidate()
                return true
            }
        })
    }

    fun setBitmaps(before: Bitmap?, after: Bitmap?) {
        this.bitmapBefore = before
        this.bitmapAfter = after
        this.sliderPosition = 0.5f
        this.scaleFactor = 1.0f
        this.panX = 0f
        this.panY = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width
        val h = height
        if (w == 0 || h == 0) return

        val bBefore = bitmapBefore
        val bAfter = bitmapAfter ?: bBefore

        if (bBefore == null && bAfter == null) {
            return
        }

        val splitX = w * sliderPosition

        canvas.save()

        // Apply synchronized pan and zoom transform
        canvas.translate(panX, panY)
        canvas.scale(scaleFactor, scaleFactor, w / 2f, h / 2f)

        // Draw BEFORE bitmap on the entire view
        bBefore?.let { bmp ->
            calculateFitRect(bmp, w, h, dstRectF)
            srcRect.set(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRectF, null)
        }

        // Draw AFTER bitmap clipped to the right of splitX
        bAfter?.let { bmp ->
            clipPath.reset()
            clipPath.addRect(splitX, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)

            canvas.save()
            canvas.clipPath(clipPath)
            calculateFitRect(bmp, w, h, dstRectF)
            srcRect.set(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRectF, null)
            canvas.restore()
        }

        canvas.restore()

        // Draw Divider Line
        canvas.drawLine(splitX, 0f, splitX, h.toFloat(), dividerPaint)

        // Draw Slider Handle Circle
        val centerY = h / 2f
        canvas.drawCircle(splitX, centerY, HANDLE_RADIUS, handleCirclePaint)
        canvas.drawCircle(splitX, centerY, HANDLE_RADIUS - 6f, handleInnerPaint)

        // Draw BEFORE Badge on top-left
        drawBadge(canvas, "BEFORE", 40f, 60f)

        // Draw AFTER Badge on top-right
        drawBadge(canvas, "AFTER", w - 160f, 60f)
    }

    private fun drawBadge(canvas: Canvas, text: String, x: Float, y: Float) {
        val badgeW = 120f
        val badgeH = 48f
        val rect = RectF(x, y, x + badgeW, y + badgeH)
        canvas.drawRoundRect(rect, 8f, 8f, badgeBgPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + 10f, badgeTextPaint)
    }

    private fun calculateFitRect(bmp: Bitmap, viewW: Int, viewH: Int, outRect: RectF) {
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()

        val scale = min(viewW.toFloat() / bw, viewH.toFloat() / bh)
        val fitW = bw * scale
        val fitH = bh * scale

        val left = (viewW - fitW) / 2f
        val top = (viewH - fitH) / 2f

        outRect.set(left, top, left + fitW, top + fitH)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val x = event.x
        val y = event.y
        val splitX = width * sliderPosition

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (Math.abs(x - splitX) <= SLIDER_TOUCH_TOLERANCE * 2) {
                    isDraggingSlider = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                } else {
                    isDraggingSlider = false
                    lastTouchX = x
                    lastTouchY = y
                    activePointerId = event.getPointerId(0)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDraggingSlider) {
                    sliderPosition = (x / width.toFloat()).coerceIn(0.05f, 0.95f)
                    invalidate()
                } else if (!scaleDetector.isInProgress && activePointerId != INVALID_POINTER_ID) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val currX = event.getX(pointerIndex)
                        val currY = event.getY(pointerIndex)
                        val dx = currX - lastTouchX
                        val dy = currY - lastTouchY

                        panX += dx
                        panY += dy

                        lastTouchX = currX
                        lastTouchY = currY
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingSlider = false
                activePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }
}
