package com.repp.aquaticremineztools.domain.compressor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.repp.aquaticremineztools.data.compressor.CompressionConfig
import com.repp.aquaticremineztools.data.compressor.CompressionMode
import com.repp.aquaticremineztools.data.compressor.OutputFormat
import com.repp.aquaticremineztools.data.compressor.ResizeMode
import com.repp.aquaticremineztools.domain.compressor.ImageMetadataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class NativeLocalCompressionProvider : CompressionProvider {

    override val name: String = "Native Engine (Local)"
    override val isCloud: Boolean = false

    override suspend fun compress(
        context: Context,
        inputFile: File,
        outputFile: File,
        config: CompressionConfig
    ): CompressionProviderResult = withContext(Dispatchers.IO) {
        try {
            if (!inputFile.exists() || inputFile.length() == 0L) {
                return@withContext CompressionProviderResult(
                    isSuccess = false,
                    outputFile = null,
                    outputWidth = 0,
                    outputHeight = 0,
                    outputFormat = "",
                    errorMessage = "Input file is empty or missing."
                )
            }

            val orientation = ImageMetadataManager.getExifOrientation(inputFile)

            val rawBoundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(inputFile.absolutePath, rawBoundsOptions)
            val srcWidth = rawBoundsOptions.outWidth
            val srcHeight = rawBoundsOptions.outHeight

            if (srcWidth <= 0 || srcHeight <= 0) {
                return@withContext CompressionProviderResult(
                    isSuccess = false,
                    outputFile = null,
                    outputWidth = 0,
                    outputHeight = 0,
                    outputFormat = "",
                    errorMessage = "Failed to decode input bitmap dimensions."
                )
            }

            // Calculate target dimensions
            var targetW = srcWidth
            var targetH = srcHeight

            if (config.enableResize) {
                when (config.resizeMode) {
                    ResizeMode.PERCENTAGE -> {
                        val pct = config.resizePercent.coerceIn(10, 100) / 100.0
                        targetW = (srcWidth * pct).roundToInt().coerceAtLeast(1)
                        targetH = (srcHeight * pct).roundToInt().coerceAtLeast(1)
                    }
                    ResizeMode.CUSTOM -> {
                        val requestedW = config.customWidth.coerceAtLeast(1)
                        val requestedH = config.customHeight.coerceAtLeast(1)
                        if (config.lockAspectRatio) {
                            val ratio = srcWidth.toDouble() / srcHeight.toDouble()
                            if (requestedW.toDouble() / requestedH.toDouble() > ratio) {
                                targetH = requestedH
                                targetW = (requestedH * ratio).roundToInt().coerceAtLeast(1)
                            } else {
                                targetW = requestedW
                                targetH = (requestedW / ratio).roundToInt().coerceAtLeast(1)
                            }
                        } else {
                            targetW = requestedW
                            targetH = requestedH
                        }
                    }
                    else -> {}
                }
            }

            val sampleSize = calculateInSampleSize(srcWidth, srcHeight, targetW, targetH)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var baseBitmap = BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions)
                ?: return@withContext CompressionProviderResult(
                    isSuccess = false,
                    outputFile = null,
                    outputWidth = 0,
                    outputHeight = 0,
                    outputFormat = "",
                    errorMessage = "Failed to decode bitmap from file."
                )

            // Apply EXIF orientation
            baseBitmap = ImageMetadataManager.applyExifOrientation(baseBitmap, orientation)

            // Scale to target dimensions if sample size didn't hit exact target
            if (baseBitmap.width != targetW || baseBitmap.height != targetH) {
                val scaled = Bitmap.createScaledBitmap(baseBitmap, targetW, targetH, true)
                if (scaled != baseBitmap) {
                    baseBitmap.recycle()
                    baseBitmap = scaled
                }
            }

            val targetFormat = determineOutputFormat(config.outputFormat, inputFile)
            val compressFormat = getCompressFormat(targetFormat)

            val finalQuality = when (config.mode) {
                CompressionMode.AUTO -> 75
                CompressionMode.QUALITY -> config.quality.coerceIn(10, 100)
                CompressionMode.PRESET -> config.preset.initialQuality
                CompressionMode.TARGET_SIZE -> {
                    findQualityForTargetSize(baseBitmap, compressFormat, config.targetSizeBytes, outputFile)
                }
            }

            if (config.mode != CompressionMode.TARGET_SIZE) {
                FileOutputStream(outputFile).use { out ->
                    baseBitmap.compress(compressFormat, finalQuality, out)
                }
            }

            baseBitmap.recycle()

            if (!outputFile.exists() || outputFile.length() == 0L) {
                return@withContext CompressionProviderResult(
                    isSuccess = false,
                    outputFile = null,
                    outputWidth = 0,
                    outputHeight = 0,
                    outputFormat = "",
                    errorMessage = "Failed to write compressed output file."
                )
            }

            if (config.stripGps) {
                ImageMetadataManager.stripGpsMetadata(outputFile)
            }

            CompressionProviderResult(
                isSuccess = true,
                outputFile = outputFile,
                outputWidth = targetW,
                outputHeight = targetH,
                outputFormat = targetFormat.extension
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CompressionProviderResult(
                isSuccess = false,
                outputFile = null,
                outputWidth = 0,
                outputHeight = 0,
                outputFormat = "",
                errorMessage = e.localizedMessage ?: "Native compression exception."
            )
        }
    }

    private fun findQualityForTargetSize(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        targetSizeBytes: Long,
        outputFile: File
    ): Int {
        var low = 10
        var high = 95
        var bestQuality = 75

        while (low <= high) {
            val mid = (low + high) / 2
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(format, mid, out)
            }

            val currentSize = outputFile.length()
            if (currentSize <= targetSizeBytes) {
                bestQuality = mid
                low = mid + 5 // Try higher quality if still under target
            } else {
                high = mid - 5 // Lower quality to reduce file size
            }
        }

        // Final encode with best quality found
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(format, bestQuality, out)
        }

        return bestQuality
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

    private fun determineOutputFormat(choice: OutputFormat, inputFile: File): OutputFormat {
        if (choice != OutputFormat.ORIGINAL) return choice
        val name = inputFile.name.lowercase()
        return when {
            name.endsWith(".png") -> OutputFormat.PNG
            name.endsWith(".webp") -> OutputFormat.WEBP
            else -> OutputFormat.JPEG
        }
    }

    private fun getCompressFormat(format: OutputFormat): Bitmap.CompressFormat {
        return when (format) {
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            else -> Bitmap.CompressFormat.JPEG
        }
    }
}
