package com.repp.aquaticremineztools.presentation.home

import android.graphics.drawable.Drawable
import android.view.ViewOutlineProvider
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

object GlassBlurHelper {
    fun setupBlur(
        blurView: BlurView,
        blurTarget: BlurTarget,
        frameClearDrawable: Drawable? = null,
        radius: Float = 16f
    ) {
        val facade = blurView.setupWith(blurTarget)
        if (frameClearDrawable != null) {
            facade.setFrameClearDrawable(frameClearDrawable)
        }
        facade.setBlurRadius(radius)
        blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        blurView.clipToOutline = true
    }
}
