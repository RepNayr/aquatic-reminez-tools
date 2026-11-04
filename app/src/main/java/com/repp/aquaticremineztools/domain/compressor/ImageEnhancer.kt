package com.repp.aquaticremineztools.domain.compressor

import android.graphics.Bitmap
import android.graphics.Color

object ImageEnhancer {

    /**
     * Applies a 3x3 Sharpen convolution kernel to accentuate edge crispness.
     * Kernel:
     * [ 0, -1,  0]
     * [-1,  5, -1]
     * [ 0, -1,  0]
     */
    fun applySharpen(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = pixels.clone()

        val kernel = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var rSum = 0f
                var gSum = 0f
                var bSum = 0f
                var kIdx = 0

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val p = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[kIdx++]
                        rSum += Color.red(p) * weight
                        gSum += Color.green(p) * weight
                        bSum += Color.blue(p) * weight
                    }
                }

                val origAlpha = Color.alpha(pixels[y * width + x])
                val r = rSum.toInt().coerceIn(0, 255)
                val g = gSum.toInt().coerceIn(0, 255)
                val b = bSum.toInt().coerceIn(0, 255)

                outPixels[y * width + x] = Color.argb(origAlpha, r, g, b)
            }
        }

        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Applies 3x3 Denoise spatial median filter to smooth out high-frequency noise.
     */
    fun applyDenoise(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = pixels.clone()

        val rWindow = IntArray(9)
        val gWindow = IntArray(9)
        val bWindow = IntArray(9)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var idx = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val p = pixels[(y + ky) * width + (x + kx)]
                        rWindow[idx] = Color.red(p)
                        gWindow[idx] = Color.green(p)
                        bWindow[idx] = Color.blue(p)
                        idx++
                    }
                }

                rWindow.sort()
                gWindow.sort()
                bWindow.sort()

                val origAlpha = Color.alpha(pixels[y * width + x])
                // Median value is index 4
                outPixels[y * width + x] = Color.argb(origAlpha, rWindow[4], gWindow[4], bWindow[4])
            }
        }

        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Applies detail preservation blend (unsharp contrast mask) to maintain high-contrast edges.
     */
    fun applyPreserveDetails(source: Bitmap): Bitmap {
        val sharpened = applySharpen(source)
        val width = source.width
        val height = source.height

        val origPixels = IntArray(width * height)
        val sharpPixels = IntArray(width * height)
        source.getPixels(origPixels, 0, width, 0, 0, width, height)
        sharpened.getPixels(sharpPixels, 0, width, 0, 0, width, height)
        sharpened.recycle()

        val outPixels = IntArray(width * height)
        val alphaBlend = 0.35f // 35% detail enhancement blend

        for (i in origPixels.indices) {
            val pOrig = origPixels[i]
            val pSharp = sharpPixels[i]

            val a = Color.alpha(pOrig)
            val r = (Color.red(pOrig) * (1f - alphaBlend) + Color.red(pSharp) * alphaBlend).toInt().coerceIn(0, 255)
            val g = (Color.green(pOrig) * (1f - alphaBlend) + Color.green(pSharp) * alphaBlend).toInt().coerceIn(0, 255)
            val b = (Color.blue(pOrig) * (1f - alphaBlend) + Color.blue(pSharp) * alphaBlend).toInt().coerceIn(0, 255)

            outPixels[i] = Color.argb(a, r, g, b)
        }

        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }
}
