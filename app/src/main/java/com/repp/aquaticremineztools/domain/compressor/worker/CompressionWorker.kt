package com.repp.aquaticremineztools.domain.compressor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.history.HistoryLogger
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import com.repp.aquaticremineztools.data.compressor.CompressorRepository
import com.repp.aquaticremineztools.data.compressor.ItemStatus
import com.repp.aquaticremineztools.data.local.compressor.CompressorDatabase
import com.repp.aquaticremineztools.domain.compressor.ImageImportManager
import com.repp.aquaticremineztools.domain.compressor.ImageQualityValidator
import com.repp.aquaticremineztools.domain.compressor.engine.CompressionProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CompressionWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "aquatic_compression_work"
        const val NOTIFICATION_ID = 10042
        const val CHANNEL_ID = "aquatic_compressor_channel"
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        createNotificationChannel()

        val repository = CompressorRepository.getInstance(appContext)
        val dao = CompressorDatabase.getInstance(appContext).compressorDao()

        try {
            safeSetForeground(createForegroundInfo("Preparing image compression queue...", 0, 0))

            while (true) {
                if (isStopped) {
                    repository.onWorkerCompleted()
                    return@withContext Result.success()
                }

                val itemEntity = dao.getNextEligibleItem() ?: break
                val item = repository.itemsFlow.value.find { it.id == itemEntity.id }
                    ?: mapEntityToItem(itemEntity)

                val inputFile = if (!item.localTempPath.isNullOrEmpty()) {
                    File(item.localTempPath)
                } else {
                    File(ImageImportManager.getTempDir(appContext), "input_${item.id}_${item.fileName}")
                }
                val outputFile = File(ImageImportManager.getTempDir(appContext), "compressed_${item.id}_${item.fileName}")

                if (!inputFile.exists()) {
                    // Update item to failed if input file missing
                    val failedItem = item.copy(
                        status = ItemStatus.FAILED,
                        errorMessage = "Temporary source file missing or removed."
                    )
                    repository.updateItemState(failedItem)
                    continue
                }

                val totalItems = dao.getAllItemsList().size
                val completedCount = dao.getAllItemsList().count { it.status == ItemStatus.COMPLETED.name }

                val statusText = "Compressing ${completedCount + 1} of $totalItems: ${item.fileName}"
                safeSetForeground(createForegroundInfo(statusText, completedCount + 1, totalItems))

                // Log Execution Start
                val logId = HistoryLogger.logExecutionStart(
                    appContext,
                    "media_image_compressor",
                    "Image Compressor",
                    "Media",
                    "Compressing image: ${item.fileName}"
                )

                val startTime = System.currentTimeMillis()

                // Processing phase
                var currentItem = item.copy(status = ItemStatus.PROCESSING, progress = 20)
                repository.updateItemState(currentItem)

                val provider = CompressionProviderRegistry.getActiveProvider(appContext)
                val config = repository.configFlow.value

                var providerResult = provider.compress(appContext, inputFile, outputFile, config)

                // Quality retry logic (up to 2 retries with lighter quality setting if quality drops severely)
                var retryCount = 0
                while (providerResult.isSuccess && retryCount < 2) {
                    val outF = providerResult.outputFile
                    if (outF != null && outF.exists() && outF.length() > 0) {
                        val metrics = ImageQualityValidator.evaluateQuality(inputFile, outF)
                        if (!metrics.isAcceptable) {
                            retryCount++
                            val lighterConfig = config.copy(
                                quality = (config.quality + 15).coerceAtMost(95)
                            )
                            providerResult = provider.compress(appContext, inputFile, outputFile, lighterConfig)
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }

                val durationMs = System.currentTimeMillis() - startTime

                if (providerResult.isSuccess && providerResult.outputFile != null && providerResult.outputFile.exists()) {
                    val outF = providerResult.outputFile
                    val outSize = outF.length()
                    val origSize = inputFile.length()

                    // Check if output is not beneficial (compressed size >= original size)
                    val isNotBeneficial = outSize >= origSize
                    val finalOutputUri = if (isNotBeneficial) inputFile.absolutePath else outF.absolutePath
                    val finalSize = if (isNotBeneficial) origSize else outSize

                    val metrics = ImageQualityValidator.evaluateQuality(inputFile, File(finalOutputUri))

                    val completedItem = currentItem.copy(
                        status = ItemStatus.COMPLETED,
                        progress = 100,
                        compressedSize = finalSize,
                        compressedUri = finalOutputUri,
                        compressedWidth = providerResult.outputWidth,
                        compressedHeight = providerResult.outputHeight,
                        compressedFormat = providerResult.outputFormat,
                        psnrDb = metrics.psnrDb,
                        ssimScore = metrics.ssim,
                        isNotBeneficial = isNotBeneficial
                    )

                    repository.updateItemState(completedItem)

                    val savingsPct = if (origSize > 0) ((origSize - finalSize).toDouble() / origSize.toDouble()) * 100 else 0.0
                    val summary = if (isNotBeneficial) {
                        "Completed: Output size was not smaller; original retained."
                    } else {
                        "Compressed ${item.fileName} (${String.format("%.1f", savingsPct)}% saved)"
                    }

                    HistoryLogger.logExecutionFinish(
                        appContext,
                        logId,
                        "SUCCESS",
                        summary,
                        durationMs
                    )
                } else {
                    val failedItem = currentItem.copy(
                        status = ItemStatus.FAILED,
                        progress = 0,
                        errorMessage = providerResult.errorMessage ?: "Compression provider failed."
                    )
                    repository.updateItemState(failedItem)

                    HistoryLogger.logExecutionFinish(
                        appContext,
                        logId,
                        "FAILED",
                        providerResult.errorMessage ?: "Compression error",
                        durationMs
                    )
                }
            }

            repository.onWorkerCompleted()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            repository.onWorkerCompleted()
            Result.failure()
        }
    }

    private suspend fun safeSetForeground(info: ForegroundInfo) {
        try {
            setForeground(info)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createForegroundInfo(message: String, current: Int, total: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Aquatic Reminez — Image Compressor")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_media)
            .setOngoing(true)
            .setProgress(total, current, total == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Image Compressor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background image compression tasks."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun mapEntityToItem(e: com.repp.aquaticremineztools.data.local.compressor.CompressorItemEntity): CompressorItem {
        return CompressorItem(
            id = e.id,
            uri = e.uri,
            fileName = e.fileName,
            fileSize = e.fileSize,
            mimeType = e.mimeType,
            width = e.width,
            height = e.height,
            sha256 = e.sha256,
            status = try { ItemStatus.valueOf(e.status) } catch (_: Exception) { ItemStatus.READY },
            queuePosition = e.queuePosition,
            progress = e.progress,
            errorMessage = e.errorMessage,
            originalSize = e.originalSize,
            compressedSize = e.compressedSize,
            compressedUri = e.compressedUri,
            compressedWidth = e.compressedWidth,
            compressedHeight = e.compressedHeight,
            compressedFormat = e.compressedFormat,
            psnrDb = e.psnrDb,
            ssimScore = e.ssimScore,
            isNotBeneficial = e.isNotBeneficial,
            savedToGalleryPath = e.savedToGalleryPath,
            localTempPath = e.localTempPath,
            addedTimestamp = e.addedTimestamp
        )
    }
}
