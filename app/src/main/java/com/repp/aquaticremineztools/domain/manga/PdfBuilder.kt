package com.repp.aquaticremineztools.domain.manga

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.coroutineContext

object PdfBuilder {

    private const val MAX_PDF_SIZE_BYTES = 20 * 1024 * 1024L // 20 MiB

    data class PdfBuildResult(
        val pdfUriString: String,
        val sizeBytes: Long,
        val pageCount: Int,
        val isTargetSizeAchieved: Boolean,
        val warningMessage: String? = null
    )

    suspend fun createPdfFromImages(
        context: Context,
        imageFiles: List<File>,
        outputFileName: String
    ): PdfBuildResult = withContext(Dispatchers.IO) {
        if (imageFiles.isEmpty()) {
            throw IllegalArgumentException("Tidak ada file gambar yang disediakan untuk membuat PDF.")
        }

        imageFiles.forEachIndexed { index, file ->
            coroutineContext.ensureActive()
            if (!file.exists() || file.length() == 0L) {
                throw IllegalStateException("Halaman ${index + 1} tidak ditemukan atau file kosong (${file.name}). PDF dibatalkan.")
            }
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                throw IllegalStateException("Halaman ${index + 1} korup atau gagal didecode. PDF dibatalkan.")
            }
        }

        val tempPdfFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$outputFileName")

        val qualities = listOf(90, 75, 60, 45, 35)
        val maxDimensions = listOf(1800, 1400, 1100, 900, 750)

        var lastSizeBytes = 0L
        var achieved = false

        try {
            for (i in qualities.indices) {
                coroutineContext.ensureActive()
                val quality = qualities[i]
                val maxDim = maxDimensions[i]

                generatePdfPass(imageFiles, tempPdfFile, quality, maxDim)

                lastSizeBytes = tempPdfFile.length()
                if (lastSizeBytes <= MAX_PDF_SIZE_BYTES) {
                    achieved = true
                    break
                }
            }

            coroutineContext.ensureActive()
            val pdfUriString = savePdfToMediaStore(context, tempPdfFile, outputFileName)
                ?: throw IOException("Gagal menyimpan PDF ke MediaStore (URI null).")

            validateStoredPdf(context, pdfUriString)

            val warning = if (!achieved) {
                String.format(
                    Locale.US,
                    "Ukuran PDF (%.1f MB) melebihi batas 20 MB meskipun telah dikompresi maksimal.",
                    lastSizeBytes / (1024f * 1024f)
                )
            } else null

            PdfBuildResult(
                pdfUriString = pdfUriString,
                sizeBytes = lastSizeBytes,
                pageCount = imageFiles.size,
                isTargetSizeAchieved = achieved,
                warningMessage = warning
            )
        } finally {
            if (tempPdfFile.exists()) {
                tempPdfFile.delete()
            }
        }
    }

    private suspend fun generatePdfPass(
        imageFiles: List<File>,
        outputFile: File,
        quality: Int,
        maxDimension: Int
    ) {
        val pdfDocument = PdfDocument()

        try {
            imageFiles.forEachIndexed { index, file ->
                coroutineContext.ensureActive()
                val compressedBitmap = decodeAndCompressBitmap(file, quality, maxDimension)
                    ?: throw IllegalStateException("Gagal memproses gambar halaman ${index + 1}")

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

        val (width, height) = options.outWidth to options.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        val largestDim = Math.max(width, height)
        if (largestDim > maxDimension) {
            sampleSize = Math.round(largestDim.toFloat() / maxDimension.toFloat())
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = Math.max(1, sampleSize)
            inPreferredConfig = Bitmap.Config.RGB_565
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

    private fun validateStoredPdf(context: Context, uriString: String) {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val firstByte = inputStream.read()
            if (firstByte == -1) {
                throw IOException("Stored PDF is empty (read back failed).")
            }
        } ?: throw IOException("Failed to open input stream for stored PDF verification: $uriString")
    }

    fun savePdfToMediaStore(context: Context, sourcePdfFile: File, displayName: String): String? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "AquaticManga")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val itemUri = resolver.insert(collection, contentValues) ?: return null
        try {
            resolver.openOutputStream(itemUri, "w")?.use { outputStream ->
                sourcePdfFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("ContentResolver.openOutputStream returned null for $itemUri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                val updated = resolver.update(itemUri, contentValues, null, null)
                if (updated <= 0) {
                    throw IOException("Failed to update MediaStore IS_PENDING status for $itemUri")
                }
            }
            return itemUri.toString()
        } catch (e: Exception) {
            try {
                resolver.delete(itemUri, null, null)
            } catch (_: Exception) {}
            return null
        }
    }
}
