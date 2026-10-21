package com.repp.aquaticremineztools.presentation.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    private var dataPoints: List<Float> = emptyList()

    fun setSparklineData(prices: List<Float>?, isPositive: Boolean) {
        dataPoints = prices ?: emptyList()
        val colorHex = if (isPositive) "#38BFA1" else "#D9535D"
        linePaint.color = Color.parseColor(colorHex)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.size < 2) return

        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        if (width <= 0 || height <= 0) return

        var min = dataPoints.minOrNull() ?: 0f
        var max = dataPoints.maxOrNull() ?: 0f
        if (max == min) {
            max += 1f
            min -= 1f
        }
        val range = max - min

        path.reset()
        val stepX = width.toFloat() / (dataPoints.size - 1)
        val verticalPadding = height * 0.12f
        val drawableHeight = height - (verticalPadding * 2)

        dataPoints.forEachIndexed { i, value ->
            val x = paddingLeft + i * stepX
            val y = paddingTop + verticalPadding + (drawableHeight - ((value - min) / range) * drawableHeight)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, linePaint)
    }
}
