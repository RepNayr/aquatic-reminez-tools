package com.repp.aquaticremineztools.presentation.compressor

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrixValues = FloatArray(9)
    private var imageMatrixCurrent = Matrix()

    private var currentScale = 1.0f
    private val minScale = 1.0f
    private val maxScale = 5.0f

    private val lastTouch = PointF()
    private val startTouch = PointF()
    private var mode = MODE_NONE

    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG = 1
        private const val MODE_ZOOM = 2
    }

    init {
        scaleType = ScaleType.MATRIX
        setupGestureDetectors()
    }

    private fun setupGestureDetectors() {
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                var scaleFactor = detector.scaleFactor
                val origScale = currentScale
                currentScale *= scaleFactor

                if (currentScale > maxScale) {
                    currentScale = maxScale
                    scaleFactor = maxScale / origScale
                } else if (currentScale < minScale) {
                    currentScale = minScale
                    scaleFactor = minScale / origScale
                }

                val viewWidth = width.toFloat()
                val viewHeight = height.toFloat()

                if (drawable != null) {
                    imageMatrixCurrent.postScale(
                        scaleFactor,
                        scaleFactor,
                        detector.focusX,
                        detector.focusY
                    )
                    fixTranslation(viewWidth, viewHeight)
                    imageMatrix = imageMatrixCurrent
                }
                return true
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val targetScale = if (currentScale > 1.2f) 1.0f else 2.5f
                val scaleFactor = targetScale / currentScale
                currentScale = targetScale

                val viewWidth = width.toFloat()
                val viewHeight = height.toFloat()

                imageMatrixCurrent.postScale(scaleFactor, scaleFactor, e.x, e.y)
                fixTranslation(viewWidth, viewHeight)
                imageMatrix = imageMatrixCurrent
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val currentPoint = PointF(event.x, event.y)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(currentPoint)
                startTouch.set(lastTouch)
                mode = MODE_DRAG
                if (currentScale > 1.0f) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = MODE_ZOOM
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == MODE_DRAG && currentScale > 1.0f) {
                    val deltaX = currentPoint.x - lastTouch.x
                    val deltaY = currentPoint.y - lastTouch.y
                    imageMatrixCurrent.postTranslate(deltaX, deltaY)
                    fixTranslation(width.toFloat(), height.toFloat())
                    imageMatrix = imageMatrixCurrent
                    lastTouch.set(currentPoint.x, currentPoint.y)
                } else if (mode == MODE_DRAG && currentScale <= 1.01f) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = MODE_NONE
                if (currentScale <= 1.01f) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return true
    }

    private fun fixTranslation(viewWidth: Float, viewHeight: Float) {
        val drawable = drawable ?: return
        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        imageMatrixCurrent.mapRect(rect)

        val rectWidth = rect.width()
        val rectHeight = rect.height()

        var deltaX = 0f
        var deltaY = 0f

        if (rectHeight < viewHeight) {
            deltaY = (viewHeight - rectHeight) / 2 - rect.top
        } else if (rect.top > 0) {
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom
        }

        if (rectWidth < viewWidth) {
            deltaX = (viewWidth - rectWidth) / 2 - rect.left
        } else if (rect.left > 0) {
            deltaX = -rect.left
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
        }

        imageMatrixCurrent.postTranslate(deltaX, deltaY)
    }

    fun resetZoom() {
        currentScale = 1.0f
        val drawable = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        imageMatrixCurrent.reset()
        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

        if (viewWidth > 0 && viewHeight > 0 && rect.width() > 0 && rect.height() > 0) {
            val scale = Math.min(viewWidth / rect.width(), viewHeight / rect.height())
            imageMatrixCurrent.postScale(scale, scale)
            val dx = (viewWidth - rect.width() * scale) / 2f
            val dy = (viewHeight - rect.height() * scale) / 2f
            imageMatrixCurrent.postTranslate(dx, dy)
        }
        imageMatrix = imageMatrixCurrent
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed && currentScale <= 1.0f) {
            resetZoom()
        }
    }
}
