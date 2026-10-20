package com.repp.aquaticremineztools.presentation.home

import android.content.Context
import android.util.AttributeSet
import eightbitlab.com.blurview.BlurView

class SquareBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BlurView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Enforce 1:1 square aspect ratio (height = width)
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
