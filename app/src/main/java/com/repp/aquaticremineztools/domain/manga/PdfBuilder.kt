package com.repp.aquaticremineztools.domain.manga

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import kotlinx.coroutines.CancellationException
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
        outputFileName: String,
        expectedPages: Int,
        isCancelledCheck: (suspend () -> Boolean)? = null,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): PdfBuildResult = withContext(Dispatchers.IO) {
        if (imageFiles.isEmpty()) {
            throw IllegalArgumentException("Tidak ada file gambar yang disediakan untuk membuat PDF.")
        }

        onProgress?.invoke(MangaDownloadEntity.STAGE_BUILDING_PDF, 5)

        imageFiles.forEachIndexed { index, file ->
            coroutineContext.ensureActive()
            if (isCancelledCheck?.invoke() == true) {
                throw CancellationException("Pembuatan PDF dibatalkan oleh pengguna.")
            }
            if (!file.exists() || file.length() == 0L) {
                throw IllegalStateException("Halaman ${index + 1} tidak ditemukan atau file kosong (${file.name}). PDF dibatalkan.")
            }
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                throw IllegalStateException("Halaman ${index + 1} korup atau gagal didecode. PDF dibatalkan.")
            }
        }

        onProgress?.invoke(MangaDownloadEntity.STAGE_BUILDING_PDF, 15)

        val tempPdfFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$outputFileName")

        // Single pass build without aggressive auto-compression during PDF Building (per Part 2 requirements)
        val quality = 90
        val maxDimension = 1600

        try {
            coroutineContext.ensureActive()
            if (isCancelledCheck?.invoke() == true) {
                throw CancellationException("Pembuatan PDF dibatalkan oleh pengguna.")
            }

            val pdfDocument = PdfDocument()
            try {
                val total = imageFiles.size
                imageFiles.forEachIndexed { index, file ->
                    coroutineContext.ensureActive()
                    if (isCancelledCheck?.invoke() == true) {
                        throw CancellationException("Pembuatan PDF dibatalkan saat proses halaman ${index + 1}.")
                    }
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

                    val percent = 15 + ((index + 1) * 65 / total)
                    onProgress?.invoke(MangaDownloadEntity.STAGE_BUILDING_PDF, percent.coerceIn(15, 80))
                }

                FileOutputStream(tempPdfFile).use { out ->
                    pdfDocument.writeTo(out)
                }
            } finally {
                pdfDocument.close()
            }

            val lastSizeBytes = tempPdfFile.length()
            val achieved = lastSizeBytes <= MAX_PDF_SIZE_BYTES

            coroutineContext.ensureActive()
            if (isCancelledCheck?.invoke() == true) {
                throw CancellationException("Pembuatan PDF dibatalkan oleh pengguna.")
            }

            onProgress?.invoke(MangaDownloadEntity.STAGE_BUILDING_PDF, 82)

            val pdfUriString = savePdfToMediaStore(context, tempPdfFile, outputFileName)
                ?: throw IOException("Gagal menyimpan PDF ke MediaStore (URI null).")

            onProgress?.invoke(MangaDownloadEntity.STAGE_VALIDATING_PDF, 92)

            try {
                if (isCancelledCheck?.invoke() == true) {
                    throw CancellationException("Pembuatan PDF dibatalkan setelah publikasi MediaStore.")
                }
                validateStoredPdf(context, pdfUriString, expectedPages)
            } catch (e: Exception) {
                try {
                    context.contentResolver.delete(Uri.parse(pdfUriString), null, null)
                } catch (_: Exception) {}
                throw e
            }

            onProgress?.invoke(MangaDownloadEntity.STAGE_VALIDATING_PDF, 100)

            val actualSizeBytes = getStoredFileSize(context, pdfUriString) ?: lastSizeBytes

            val warning = if (!achieved) {
                String.format(
                    Locale.US,
                    "Ukuran PDF (%.1f MB) melebihi batas 20 MB.",
                    actualSizeBytes / (1024f * 1024f)
                )
            } else null

            PdfBuildResult(
                pdfUriString = pdfUriString,
                sizeBytes = actualSizeBytes,
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

    suspend fun compressExistingPdf(
        context: Context,
        sourcePdfFile: File,
        outputFileName: String,
        quality: Int,
        maxDimension: Int,
        maxSizeBytes: Long,
        isCancelledCheck: (suspend () -> Boolean)? = null,
        onProgress: (suspend (stage: String, percent: Int) -> Unit)? = null
    ): PdfBuildResult = withContext(Dispatchers.IO) {
        onProgress?.invoke(MangaDownloadEntity.STAGE_BUILDING_PDF, 10)

        val uri = Uri.fromFile(sourcePdfFile)
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Gagal membuka file PDF sumber untuk kompresi.")

        val tempImageFiles = mutableListOf<File>()
        val tempDir = File(context.cacheDir, "compress_temp_${System.currentTimeMillis()}").apply { mkdirs() }
        val tempOutputPdf = File(context.cacheDir, "temp_compressed_${System.currentTimeMillis()}_$outputFileName")

        try {
            PdfRenderer(pfd).use { renderer ->
                val pageCount = renderer.pageCount
                for (i in 0 until pageCount) {
                    coroutineContext.ensureActive()
                    if (isCancelledCheck?.invoke() == true) {
                        throw CancellationException("Kompresi dibatalkan oleh pengguna.")
                    }
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val imgFile = File(tempDir, "page_$i.jpg")
                        FileOutputStream(imgFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                        }
                        bitmap.recycle()
                        tempImageFiles.add(imgFile)
                    }
                    val progressPercent = 10 + ((i + 1) * 40 / pageCount)
                    onProgress?.invoke(MangaDownloadEntity.STAGE_BUILDING_PDF, progressPercent)
                }
            }

            onProgress?.invoke(MangaDownloadEntity.STAGE_COMPRESSING_PDF, 55)

            val pdfDocument = PdfDocument()
            try {
                val total = tempImageFiles.size
                tempImageFiles.forEachIndexed { index, file ->
                    coroutineContext.ensureActive()
                    if (isCancelledCheck?.invoke() == true) {
                        throw CancellationException("Kompresi dibatalkan saat proses halaman ${index + 1}.")
                    }
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

                    val percent = 55 + ((index + 1) * 30 / total)
                    onProgress?.invoke(MangaDownloadEntity.STAGE_COMPRESSING_PDF, percent.coerceIn(55, 85))
                }

                FileOutputStream(tempOutputPdf).use { out ->
                    pdfDocument.writeTo(out)
                }
            } finally {
                pdfDocument.close()
            }

            coroutineContext.ensureActive()
            if (isCancelledCheck?.invoke() == true) {
                throw CancellationException("Kompresi dibatalkan oleh pengguna.")
            }

            onProgress?.invoke(MangaDownloadEntity.STAGE_VALIDATING_PDF, 88)

            val pdfUriString = savePdfToMediaStore(context, tempOutputPdf, outputFileName)
                ?: throw IOException("Gagal menyimpan PDF hasil kompresi ke MediaStore.")

            onProgress?.invoke(MangaDownloadEntity.STAGE_VALIDATING_PDF, 95)

            try {
                if (isCancelledCheck?.invoke() == true) {
                    throw CancellationException("Kompresi dibatalkan setelah publikasi MediaStore.")
                }
                validateStoredPdf(context, pdfUriString, tempImageFiles.size)
            } catch (e: Exception) {
                try {
                    context.contentResolver.delete(Uri.parse(pdfUriString), null, null)
                } catch (_: Exception) {}
                throw e
            }

            onProgress?.invoke(MangaDownloadEntity.STAGE_VALIDATING_PDF, 100)

            val actualSizeBytes = getStoredFileSize(context, pdfUriString) ?: tempOutputPdf.length()
            val achieved = actualSizeBytes <= maxSizeBytes

            val warning = if (!achieved) {
                String.format(
                    Locale.US,
                    "Ukuran PDF (%.1f MB) masih melebihi target %.1f MB.",
                    actualSizeBytes / (1024f * 1024f),
                    maxSizeBytes / (1024f * 1024f)
                )
            } else null

            PdfBuildResult(
                pdfUriString = pdfUriString,
                sizeBytes = actualSizeBytes,
                pageCount = tempImageFiles.size,
                isTargetSizeAchieved = achieved,
                warningMessage = warning
            )
        } finally {
            pfd.close()
            tempDir.deleteRecursively()
            if (tempOutputPdf.exists()) {
                tempOutputPdf.delete()
            }
        }
    }

    private suspend fun generatePdfPass(
        imageFiles: List<File>,
        outputFile: File,
        quality: Int,
        maxDimension: Int,
        isCancelledCheck: (suspend () -> Boolean)? = null,
        onPassProgress: ((Int) -> Unit)? = null
    ) {
        val pdfDocument = PdfDocument()

        try {
            val total = imageFiles.size
            imageFiles.forEachIndexed { index, file ->
                coroutineContext.ensureActive()
                if (isCancelledCheck?.invoke() == true) {
                    throw CancellationException("Pembuatan PDF dibatalkan saat kompresi halaman ${index + 1}.")
                }
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

                val percent = ((index + 1) * 100) / total
                onPassProgress?.invoke(percent)
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

    private fun validateStoredPdf(context: Context, uriString: String, expectedPages: Int) {
        val uri = Uri.parse(uriString)
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Stored PDF file descriptor is null (read back failed): $uriString")

        pfd.use { parcelFileDescriptor ->
            if (parcelFileDescriptor.statSize <= 0L) {
                throw IOException("Stored PDF is empty (size 0): $uriString")
            }
            PdfRenderer(parcelFileDescriptor).use { renderer ->
                if (renderer.pageCount != expectedPages) {
                    throw IOException("Stored PDF page count (${renderer.pageCount}) does not match expected page count ($expectedPages).")
                }
                if (renderer.pageCount > 0) {
                    renderer.openPage(0).use { page ->
                        if (page.width <= 0 || page.height <= 0) {
                            throw IOException("Stored PDF first page dimensions are invalid.")
                        }
                    }
                }
            }
        }
    }

    private fun getStoredFileSize(context: Context, uriString: String): Long? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            }
        } catch (_: Exception) {
            null
        }
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
