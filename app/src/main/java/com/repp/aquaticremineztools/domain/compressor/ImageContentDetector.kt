package com.repp.aquaticremineztools.domain.compressor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File

enum class ImageCategory(val displayName: String) {
    PHOTO("Photo"),
    SCREENSHOT("Screenshot"),
    ILLUSTRATION("Illustration"),
    DOCUMENT("Document"),
    TRANSPARENT("Transparent Image")
}

object ImageContentDetector {

    /**
     * Analyzes image characteristics (transparency, color distribution, brightness contrast,
     * and edge sharpness) on a downsampled 256x256 thumbnail for fast heuristic classification.
     */
    fun detectCategory(file: File): ImageCategory {
        if (!file.exists() || file.length() == 0L) {
            return ImageCategory.PHOTO
        }

        var bitmap: Bitmap? = null
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val width = options.outWidth
            val height = options.outHeight
            val mimeType = options.outMimeType ?: ""

            if (width <= 0 || height <= 0) return ImageCategory.PHOTO

            // 1. Check transparency in PNG/WEBP
            if (mimeType.contains("png") || mimeType.contains("webp")) {
                options.inJustDecodeBounds = false
                options.inSampleSize = calculateInSampleSize(width, height, 256, 256)
                bitmap = BitmapFactory.decodeFile(file.absolutePath, options)

                if (bitmap != null && bitmap.hasAlpha()) {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    for (pixel in pixels) {
                        if (Color.alpha(pixel) < 250) {
                            bitmap.recycle()
                            return ImageCategory.TRANSPARENT
                        }
                    }
                }
            }

            if (bitmap == null) {
                options.inJustDecodeBounds = false
                options.inSampleSize = calculateInSampleSize(width, height, 256, 256)
                bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            }

            if (bitmap == null) return ImageCategory.PHOTO

            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            bitmap.recycle()

            val totalPixels = pixels.size
            if (totalPixels == 0) return ImageCategory.PHOTO

            // Heuristic analysis metrics
            var highLuminanceCount = 0
            var darkPixelCount = 0
            val uniqueColors = HashSet<Int>(1024)

            for (p in pixels) {
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                if (lum > 225) highLuminanceCount++
                if (lum < 40) darkPixelCount++

                // Quantize color to 15-bit RGB for fast unique palette counting
                val quantized = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
                if (uniqueColors.size < 1000) {
                    uniqueColors.add(quantized)
                }
            }

            val whiteRatio = highLuminanceCount.toDouble() / totalPixels.toDouble()
            val darkRatio = darkPixelCount.toDouble() / totalPixels.toDouble()
            val uniqueColorCount = uniqueColors.size

            // 2. Document detection: High white/light background (>65%) + dark text pixels (>10%)
            if (whiteRatio > 0.65 && darkRatio > 0.10 && uniqueColorCount < 400) {
                return ImageCategory.DOCUMENT
            }

            // 3. Screenshot vs Illustration vs Photo based on unique color diversity
            return when {
                uniqueColorCount < 150 -> ImageCategory.ILLUSTRATION
                uniqueColorCount < 450 && whiteRatio > 0.40 -> ImageCategory.SCREENSHOT
                else -> ImageCategory.PHOTO
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ImageCategory.PHOTO
        }
    }

    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
