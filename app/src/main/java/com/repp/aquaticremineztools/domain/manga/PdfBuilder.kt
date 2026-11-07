package com.repp.aquaticremineztools.domain.manga

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfBuilder {

    private const val MAX_PDF_SIZE_BYTES = 20 * 1024 * 1024L // 20 MiB

    data class PdfBuildResult(
        val pdfFile: File,
        val sizeBytes: Long,
        val isTargetSizeAchieved: Boolean,
        val warningMessage: String? = null
    )

    suspend fun createPdfFromImages(
        imageFiles: List<File>,
        outputPdfFile: File
    ): PdfBuildResult = withContext(Dispatchers.IO) {
        if (imageFiles.isEmpty()) {
            throw IllegalArgumentException("No image files provided for PDF creation.")
        }

        val qualities = listOf(90, 75, 60, 45, 35)
        val maxDimensions = listOf(1800, 1400, 1100, 900, 750)

        var lastResultFile: File = outputPdfFile
        var lastSizeBytes = 0L
        var achieved = false

        for (i in qualities.indices) {
            val quality = qualities[i]
            val maxDim = maxDimensions[i]

            generatePdfPass(imageFiles, outputPdfFile, quality, maxDim)

            lastSizeBytes = outputPdfFile.length()
            if (lastSizeBytes <= MAX_PDF_SIZE_BYTES) {
                achieved = true
                break
            }
        }

        val warning = if (!achieved) {
            "PDF size (${String.format("%.1f", lastSizeBytes / (1024f * 1024f))} MB) exceeded 20 MB limit even after maximum compression."
        } else null

        PdfBuildResult(
            pdfFile = outputPdfFile,
            sizeBytes = lastSizeBytes,
            isTargetSizeAchieved = achieved,
            warningMessage = warning
        )
    }

    private fun generatePdfPass(
        imageFiles: List<File>,
        outputFile: File,
        quality: Int,
        maxDimension: Int
    ) {
        val pdfDocument = PdfDocument()

        try {
            imageFiles.forEachIndexed { index, file ->
                if (!file.exists() || file.length() == 0L) return@forEachIndexed

                val compressedBitmap = decodeAndCompressBitmap(file, quality, maxDimension) ?: return@forEachIndexed

                val pageInfo = PdfDocument.PageInfo.Builder(
                    compressedBitmap.width,
                    compressedBitmap.height,
                    index + 1
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                val paint = Paint()

                canvas.drawBitmap(compressedBitmap, 0f, 0f, paint)
                pdfDocument.finishPage(page)

                compressedBitmap.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
    }

    private fun decodeAndCompressBitmap(file: File, quality: Int, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var sampleSize = 1
        val (width, height) = options.outWidth to options.outHeight
        if (width <= 0 || height <= 0) return null

        val largestDim = Math.max(width, height)
        if (largestDim > maxDimension) {
            sampleSize = Math.round(largestDim.toFloat() / maxDimension.toFloat())
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = Math.max(1, sampleSize)
            inPreferredConfig = Bitmap.Config.RGB_565 // Memory saving
        }

        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

        if (quality >= 100) {
            return originalBitmap
        }

        val stream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        originalBitmap.recycle()

        val compressedBytes = stream.toByteArray()
        return BitmapFactory.decodeStream(ByteArrayInputStream(compressedBytes))
    }
}
