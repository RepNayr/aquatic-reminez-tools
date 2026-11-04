package com.repp.aquaticremineztools.domain.compressor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

data class QualityMetrics(
    val mse: Double?,
    val psnrDb: Double?,
    val ssim: Double?,
    val isAcceptable: Boolean
)

object ImageQualityValidator {

    private const val MIN_ACCEPTABLE_PSNR_DB = 25.0
    private const val MIN_ACCEPTABLE_SSIM = 0.70

    fun evaluateQuality(originalFile: File, compressedFile: File): QualityMetrics {
        var origBitmap: Bitmap? = null
        var compBitmap: Bitmap? = null
        var b1: Bitmap? = null
        var b2: Bitmap? = null

        try {
            origBitmap = decodeSampledBitmap(originalFile, 512, 512)
            compBitmap = decodeSampledBitmap(compressedFile, 512, 512)

            if (origBitmap == null || compBitmap == null) {
                return QualityMetrics(null, null, null, false)
            }

            // Scale to same comparison dimensions if different
            val evalWidth = 256
            val evalHeight = 256
            b1 = Bitmap.createScaledBitmap(origBitmap, evalWidth, evalHeight, true)
            b2 = Bitmap.createScaledBitmap(compBitmap, evalWidth, evalHeight, true)

            val pixels1 = IntArray(evalWidth * evalHeight)
            val pixels2 = IntArray(evalWidth * evalHeight)

            b1.getPixels(pixels1, 0, evalWidth, 0, 0, evalWidth, evalHeight)
            b2.getPixels(pixels2, 0, evalWidth, 0, 0, evalWidth, evalHeight)

            val mse = calculateMse(pixels1, pixels2)
            val psnr = calculatePsnr(mse)
            val ssim = calculateSsim(pixels1, pixels2, evalWidth, evalHeight)

            val isAcceptable = psnr >= MIN_ACCEPTABLE_PSNR_DB && ssim >= MIN_ACCEPTABLE_SSIM

            return QualityMetrics(
                mse = mse,
                psnrDb = psnr,
                ssim = ssim,
                isAcceptable = isAcceptable
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return QualityMetrics(null, null, null, false)
        } finally {
            b1?.takeIf { it != origBitmap }?.recycle()
            b2?.takeIf { it != compBitmap }?.recycle()
            origBitmap?.recycle()
            compBitmap?.recycle()
        }
    }

    private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun calculateMse(pixels1: IntArray, pixels2: IntArray): Double {
        var sumDiffSq = 0.0
        val count = pixels1.size
        for (i in 0 until count) {
            val p1 = pixels1[i]
            val p2 = pixels2[i]

            val r1 = Color.red(p1)
            val g1 = Color.green(p1)
            val b1 = Color.blue(p1)

            val r2 = Color.red(p2)
            val g2 = Color.green(p2)
            val b2 = Color.blue(p2)

            val diffR = (r1 - r2).toDouble()
            val diffG = (g1 - g2).toDouble()
            val diffB = (b1 - b2).toDouble()

            sumDiffSq += (diffR * diffR + diffG * diffG + diffB * diffB) / 3.0
        }
        return sumDiffSq / count
    }

    private fun calculatePsnr(mse: Double): Double {
        if (mse <= 0.00001) return 100.0 // Near identical
        val maxVal = 255.0
        return 10.0 * kotlin.math.log10((maxVal * maxVal) / mse)
    }

    private fun calculateSsim(pixels1: IntArray, pixels2: IntArray, width: Int, height: Int): Double {
        val n = pixels1.size
        var mean1 = 0.0
        var mean2 = 0.0

        for (i in 0 until n) {
            val lum1 = 0.299 * Color.red(pixels1[i]) + 0.587 * Color.green(pixels1[i]) + 0.114 * Color.blue(pixels1[i])
            val lum2 = 0.299 * Color.red(pixels2[i]) + 0.587 * Color.green(pixels2[i]) + 0.114 * Color.blue(pixels2[i])
            mean1 += lum1
            mean2 += lum2
        }
        mean1 /= n
        mean2 /= n

        var var1 = 0.0
        var var2 = 0.0
        var covar = 0.0

        for (i in 0 until n) {
            val lum1 = 0.299 * Color.red(pixels1[i]) + 0.587 * Color.green(pixels1[i]) + 0.114 * Color.blue(pixels1[i])
            val lum2 = 0.299 * Color.red(pixels2[i]) + 0.587 * Color.green(pixels2[i]) + 0.114 * Color.blue(pixels2[i])

            val diff1 = lum1 - mean1
            val diff2 = lum2 - mean2

            var1 += diff1 * diff1
            var2 += diff2 * diff2
            covar += diff1 * diff2
        }

        var1 /= (n - 1)
        var2 /= (n - 1)
        covar /= (n - 1)

        val c1 = (0.01 * 255.0).pow(2)
        val c2 = (0.03 * 255.0).pow(2)

        val num = (2 * mean1 * mean2 + c1) * (2 * covar + c2)
        val den = (mean1.pow(2) + mean2.pow(2) + c1) * (var1 + var2 + c2)

        return if (den != 0.0) num / den else 1.0
    }
}
