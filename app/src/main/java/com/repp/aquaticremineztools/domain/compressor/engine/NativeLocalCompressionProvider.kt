package com.repp.aquaticremineztools.domain.compressor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import com.repp.aquaticremineztools.data.compressor.CompressionConfig
import com.repp.aquaticremineztools.data.compressor.CompressionMode
import com.repp.aquaticremineztools.data.compressor.CompressionPreset
import com.repp.aquaticremineztools.data.compressor.OutputFormat
import com.repp.aquaticremineztools.data.compressor.ResizeMode
import com.repp.aquaticremineztools.data.compressor.ResolutionPreset
import com.repp.aquaticremineztools.data.compressor.TargetSizePriority
import com.repp.aquaticremineztools.domain.compressor.ImageCategory
import com.repp.aquaticremineztools.domain.compressor.ImageContentDetector
import com.repp.aquaticremineztools.domain.compressor.ImageEnhancer
import com.repp.aquaticremineztools.domain.compressor.ImageMetadataManager
import com.repp.aquaticremineztools.domain.compressor.ImageQualityValidator
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
        val startTime = System.currentTimeMillis()
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

            val originalFileSize = inputFile.length()

            // 1. Image Content Category Detection (Photo, Screenshot, Illustration, Document, Transparent)
            val detectedCategory = ImageContentDetector.detectCategory(inputFile)

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

            // 2. Calculate target dimensions based on Resolution Preset or Resize Mode (Never upscale)
            val (targetW, targetH) = calculateTargetDimensions(srcWidth, srcHeight, config)

            val sampleSize = calculateInSampleSize(srcWidth, srcHeight, targetW, targetH)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var baseBitmap: Bitmap? = try {
                BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions)
            } catch (oom: OutOfMemoryError) {
                decodeOptions.inSampleSize = sampleSize * 2
                BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions)
            }

            if (baseBitmap == null) {
                return@withContext CompressionProviderResult(
                    isSuccess = false,
                    outputFile = null,
                    outputWidth = 0,
                    outputHeight = 0,
                    outputFormat = "",
                    errorMessage = "Failed to decode bitmap from file."
                )
            }

            // Apply EXIF orientation
            baseBitmap = ImageMetadataManager.applyExifOrientation(baseBitmap, orientation)

            // Scale to target dimensions if sample size didn't hit exact target
            if (baseBitmap.width != targetW || baseBitmap.height != targetH) {
                val scaled = try {
                    Bitmap.createScaledBitmap(baseBitmap, targetW, targetH, true)
                } catch (oom: OutOfMemoryError) {
                    baseBitmap
                }
                if (scaled != baseBitmap) {
                    baseBitmap.recycle()
                    baseBitmap = scaled
                }
            }

            // 3. Apply Optional Enhancement Filters (Sharpen, Denoise, Preserve Fine Details)
            baseBitmap = applyEnhancementsIfNeeded(baseBitmap, config)

            // 4. Determine output format & transparency handling
            val hasAlpha = baseBitmap.hasAlpha() || detectedCategory == ImageCategory.TRANSPARENT
            val targetFormat = determineOutputFormat(config.outputFormat, inputFile, hasAlpha, detectedCategory)

            // Prepare working bitmap (if JPEG and alpha present, flatten onto white canvas)
            val processBitmap = if (targetFormat == OutputFormat.JPEG && hasAlpha) {
                flattenAlphaOntoWhite(baseBitmap)
            } else {
                baseBitmap
            }

            // 5. Smart Adaptive initial quality calculation based on category & megapixels
            val adaptiveAutoQuality = calculateSmartAutoQuality(srcWidth, srcHeight, originalFileSize, detectedCategory)

            // Strategy Pass Trial Loop (up to 3 passes)
            data class TrialResult(
                val tempFile: File,
                val format: OutputFormat,
                val quality: Int,
                val size: Long,
                val psnr: Double?,
                val ssim: Double?,
                val isAcceptable: Boolean,
                val targetAchieved: Boolean
            )

            val trialResults = mutableListOf<TrialResult>()
            val parentDir = outputFile.parentFile ?: context.cacheDir

            val minQualityFloor = config.minQuality.coerceIn(10, 95)

            for (pass in 1..3) {
                val (trialFormat, trialQuality) = when (pass) {
                    1 -> {
                        val initialQuality = when (config.mode) {
                            CompressionMode.AUTO -> adaptiveAutoQuality
                            CompressionMode.QUALITY -> config.quality.coerceIn(minQualityFloor, 100)
                            CompressionMode.PRESET -> {
                                when (config.preset) {
                                    CompressionPreset.LIGHT -> 88
                                    CompressionPreset.BALANCED, CompressionPreset.MEDIUM -> 75
                                    CompressionPreset.STRONG, CompressionPreset.HIGH -> 55
                                    CompressionPreset.EXTREME, CompressionPreset.MAXIMUM -> 35
                                    CompressionPreset.SMART_AUTO -> adaptiveAutoQuality
                                    CompressionPreset.CUSTOM -> config.quality.coerceIn(minQualityFloor, 100)
                                    CompressionPreset.LOW -> 90
                                }
                            }
                            CompressionMode.TARGET_SIZE -> {
                                val compressFmt = getCompressFormat(targetFormat)
                                findQualityForTargetSize(processBitmap, compressFmt, config.targetSizeBytes, outputFile, minQualityFloor)
                            }
                        }
                        Pair(targetFormat, initialQuality)
                    }
                    2 -> {
                        val pass1 = trialResults.firstOrNull()
                        if (pass1 != null && pass1.size >= originalFileSize) {
                            if (pass1.format == OutputFormat.PNG && (config.outputFormat == OutputFormat.SMART || config.outputFormat == OutputFormat.ORIGINAL)) {
                                Pair(OutputFormat.WEBP, 80)
                            } else {
                                Pair(pass1.format, (pass1.quality - 18).coerceAtLeast(minQualityFloor))
                            }
                        } else if (pass1 != null && !pass1.isAcceptable) {
                            Pair(pass1.format, (pass1.quality + 15).coerceAtMost(95))
                        } else {
                            Pair(targetFormat, (adaptiveAutoQuality - 15).coerceAtLeast(minQualityFloor))
                        }
                    }
                    3 -> {
                        val pass2 = trialResults.lastOrNull()
                        if (pass2 != null && pass2.size >= originalFileSize) {
                            val fmt = if (!hasAlpha) OutputFormat.JPEG else OutputFormat.WEBP
                            Pair(fmt, (65).coerceAtLeast(minQualityFloor))
                        } else {
                            Pair(targetFormat, (70).coerceAtLeast(minQualityFloor))
                        }
                    }
                    else -> Pair(targetFormat, 75)
                }

                val trialFile = File(parentDir, "trial_${pass}_${outputFile.name}")
                val compressFormat = getCompressFormat(trialFormat)

                try {
                    FileOutputStream(trialFile).use { out ->
                        processBitmap.compress(compressFormat, trialQuality, out)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    continue
                }

                if (!trialFile.exists() || trialFile.length() == 0L) {
                    trialFile.delete()
                    continue
                }

                val trialMetrics = ImageQualityValidator.evaluateQuality(inputFile, trialFile)
                val trialSize = trialFile.length()
                val targetAchieved = config.mode != CompressionMode.TARGET_SIZE || trialSize <= config.targetSizeBytes

                trialResults.add(
                    TrialResult(
                        tempFile = trialFile,
                        format = trialFormat,
                        quality = trialQuality,
                        size = trialSize,
                        psnr = trialMetrics.psnrDb,
                        ssim = trialMetrics.ssim,
                        isAcceptable = trialMetrics.isAcceptable,
                        targetAchieved = targetAchieved
                    )
                )

                if (trialSize < originalFileSize && trialMetrics.isAcceptable && targetAchieved) {
                    break
                }
            }

            if (processBitmap != baseBitmap) {
                processBitmap.recycle()
            }
            baseBitmap.recycle()

            // 6. Select candidate winner based on TargetSizePriority and visual quality
            val validCandidates = trialResults.filter { it.size < originalFileSize }

            val validWinner = when (config.targetSizePriority) {
                TargetSizePriority.SIZE_FIRST -> {
                    validCandidates.filter { it.targetAchieved }.minByOrNull { it.size }
                        ?: validCandidates.minByOrNull { it.size }
                }
                TargetSizePriority.QUALITY_FIRST -> {
                    validCandidates.filter { it.isAcceptable }.maxByOrNull { it.quality }
                        ?: validCandidates.firstOrNull()
                }
                TargetSizePriority.BALANCED -> {
                    validCandidates.filter { it.isAcceptable && it.targetAchieved }.minByOrNull { it.size }
                        ?: validCandidates.filter { it.isAcceptable }.minByOrNull { it.size }
                        ?: validCandidates.minByOrNull { it.size }
                }
            }

            val durationMs = System.currentTimeMillis() - startTime

            if (validWinner != null) {
                validWinner.tempFile.copyTo(outputFile, overwrite = true)
                trialResults.forEach { it.tempFile.delete() }

                ImageMetadataManager.stripAllMetadata(outputFile)

                return@withContext CompressionProviderResult(
                    isSuccess = true,
                    outputFile = outputFile,
                    outputWidth = targetW,
                    outputHeight = targetH,
                    outputFormat = validWinner.format.extension,
                    isNotBeneficial = false,
                    detectedCategory = detectedCategory.displayName,
                    targetSizeAchieved = validWinner.targetAchieved,
                    processingTimeMs = durationMs
                )
            } else {
                trialResults.forEach { it.tempFile.delete() }

                return@withContext CompressionProviderResult(
                    isSuccess = true,
                    outputFile = null,
                    outputWidth = targetW,
                    outputHeight = targetH,
                    outputFormat = targetFormat.extension,
                    isNotBeneficial = true,
                    detectedCategory = detectedCategory.displayName,
                    targetSizeAchieved = false,
                    processingTimeMs = durationMs
                )
            }
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

    private fun calculateTargetDimensions(srcW: Int, srcH: Int, config: CompressionConfig): Pair<Int, Int> {
        var targetW = srcW
        var targetH = srcH

        // Resolution Presets
        if (config.resolutionPreset != ResolutionPreset.ORIGINAL && config.resolutionPreset != ResolutionPreset.CUSTOM) {
            val maxLongSide = config.resolutionPreset.targetLongSide
            val currentLongSide = kotlin.math.max(srcW, srcH)
            if (currentLongSide > maxLongSide) {
                val ratio = srcW.toDouble() / srcH.toDouble()
                if (srcW >= srcH) {
                    targetW = maxLongSide
                    targetH = (maxLongSide / ratio).roundToInt().coerceAtLeast(1)
                } else {
                    targetH = maxLongSide
                    targetW = (maxLongSide * ratio).roundToInt().coerceAtLeast(1)
                }
            }
            return Pair(targetW, targetH)
        }

        // Custom Resize
        if (config.enableResize) {
            when (config.resizeMode) {
                ResizeMode.PERCENTAGE -> {
                    val pct = config.resizePercent.coerceIn(10, 100) / 100.0
                    val requestedW = (srcW * pct).roundToInt().coerceAtLeast(1)
                    val requestedH = (srcH * pct).roundToInt().coerceAtLeast(1)
                    targetW = requestedW.coerceAtMost(srcW)
                    targetH = requestedH.coerceAtMost(srcH)
                }
                ResizeMode.CUSTOM -> {
                    val requestedW = config.customWidth.coerceAtLeast(1)
                    val requestedH = config.customHeight.coerceAtLeast(1)
                    if (config.lockAspectRatio) {
                        val ratio = srcW.toDouble() / srcH.toDouble()
                        if (requestedW.toDouble() / requestedH.toDouble() > ratio) {
                            targetH = requestedH.coerceAtMost(srcH)
                            targetW = (targetH * ratio).roundToInt().coerceAtLeast(1).coerceAtMost(srcW)
                        } else {
                            targetW = requestedW.coerceAtMost(srcW)
                            targetH = (targetW / ratio).roundToInt().coerceAtLeast(1).coerceAtMost(srcH)
                        }
                    } else {
                        targetW = requestedW.coerceAtMost(srcW)
                        targetH = requestedH.coerceAtMost(srcH)
                    }
                }
                else -> {}
            }
        }

        return Pair(targetW, targetH)
    }

    private fun applyEnhancementsIfNeeded(source: Bitmap, config: CompressionConfig): Bitmap {
        var result = source
        if (config.enableSharpen) {
            val sharpened = ImageEnhancer.applySharpen(result)
            if (sharpened != result) {
                if (result != source) result.recycle()
                result = sharpened
            }
        }
        if (config.enableDenoise) {
            val denoised = ImageEnhancer.applyDenoise(result)
            if (denoised != result) {
                if (result != source) result.recycle()
                result = denoised
            }
        }
        if (config.enablePreserveDetails) {
            val detailed = ImageEnhancer.applyPreserveDetails(result)
            if (detailed != result) {
                if (result != source) result.recycle()
                result = detailed
            }
        }
        return result
    }

    private fun calculateSmartAutoQuality(srcW: Int, srcH: Int, fileSize: Long, category: ImageCategory): Int {
        val megapixels = (srcW.toLong() * srcH.toLong()) / 1_000_000.0
        val sizeMb = fileSize / (1024.0 * 1024.0)

        return when (category) {
            ImageCategory.DOCUMENT -> 75
            ImageCategory.SCREENSHOT -> 82
            ImageCategory.ILLUSTRATION -> 85
            ImageCategory.TRANSPARENT -> 85
            ImageCategory.PHOTO -> {
                when {
                    megapixels >= 12.0 || sizeMb >= 8.0 -> 72
                    megapixels >= 4.0 || sizeMb >= 2.0 -> 78
                    else -> 84
                }
            }
        }
    }

    private fun flattenAlphaOntoWhite(source: Bitmap): Bitmap {
        val flattened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, 0f, 0f, null)
        return flattened
    }

    private fun findQualityForTargetSize(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        targetSizeBytes: Long,
        outputFile: File,
        minQualityFloor: Int
    ): Int {
        var low = minQualityFloor
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
                low = mid + 5
            } else {
                high = mid - 5
            }
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

    private fun determineOutputFormat(
        choice: OutputFormat,
        inputFile: File,
        hasAlpha: Boolean,
        category: ImageCategory
    ): OutputFormat {
        return when (choice) {
            OutputFormat.SMART, OutputFormat.AUTO_BEST -> {
                if (hasAlpha) OutputFormat.WEBP else if (category == ImageCategory.SCREENSHOT || category == ImageCategory.ILLUSTRATION) OutputFormat.WEBP else OutputFormat.JPEG
            }
            OutputFormat.ORIGINAL -> {
                val name = inputFile.name.lowercase()
                when {
                    name.endsWith(".png") -> OutputFormat.PNG
                    name.endsWith(".webp") -> OutputFormat.WEBP
                    name.endsWith(".avif") -> OutputFormat.AVIF
                    else -> OutputFormat.JPEG
                }
            }
            else -> choice
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
            OutputFormat.AVIF -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        Bitmap.CompressFormat.valueOf("AVIF")
                    } catch (_: Exception) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
            }
            else -> Bitmap.CompressFormat.JPEG
        }
    }
}
