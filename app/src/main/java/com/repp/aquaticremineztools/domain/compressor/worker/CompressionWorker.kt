package com.repp.aquaticremineztools.domain.compressor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.repp.aquaticremineztools.domain.compressor.GallerySaveManager
import com.repp.aquaticremineztools.domain.compressor.ImageImportManager
import com.repp.aquaticremineztools.domain.compressor.ImageQualityValidator
import com.repp.aquaticremineztools.domain.compressor.StorageManager
import com.repp.aquaticremineztools.domain.compressor.TempFileManager
import com.repp.aquaticremineztools.domain.compressor.engine.CompressionProviderRegistry
import com.repp.aquaticremineztools.presentation.compressor.ImageCompressorActivity
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

            // Storage Pre-check before executing batch
            val eligibleEntities = dao.getAllItemsList().filter {
                it.status in listOf(
                    ItemStatus.QUEUED.name,
                    ItemStatus.PROCESSING.name,
                    ItemStatus.PREPARING.name
                )
            }
            val totalBatchBytes = eligibleEntities.sumOf { it.fileSize }
            val storageCheck = StorageManager.checkAvailableStorage(appContext, totalBatchBytes)

            if (!storageCheck.isSufficient) {
                for (entity in eligibleEntities) {
                    val item = mapEntityToItem(entity)
                    val failedItem = item.copy(
                        status = ItemStatus.FAILED,
                        errorMessage = storageCheck.warningMessage ?: "Insufficient storage space on device."
                    )
                    repository.updateItemState(failedItem)
                }
                repository.onWorkerCompleted()
                return@withContext Result.failure()
            }

            val activePaths = mutableSetOf<String>()

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

                inputFile.absolutePath.let { activePaths.add(it) }
                outputFile.absolutePath.let { activePaths.add(it) }

                if (!inputFile.exists()) {
                    val failedItem = item.copy(
                        status = ItemStatus.FAILED,
                        errorMessage = "Temporary source file missing or removed."
                    )
                    repository.updateItemState(failedItem)
                    continue
                }

                val allList = dao.getAllItemsList()
                val totalItems = allList.size
                val completedCount = allList.count { it.status == ItemStatus.COMPLETED.name || it.status == ItemStatus.SAVED.name }

                val statusText = "Compressing ${completedCount + 1} of $totalItems: ${item.fileName}"
                safeSetForeground(createForegroundInfo(statusText, completedCount + 1, totalItems))

                val logId = HistoryLogger.logExecutionStart(
                    appContext,
                    "media_image_compressor",
                    "Image Compressor",
                    "Media",
                    "Compressing image: ${item.fileName}"
                )

                val startTime = System.currentTimeMillis()

                // State pipeline transition: PREPARING -> COMPRESSING
                var currentItem = item.copy(status = ItemStatus.PROCESSING, progress = 20)
                repository.updateItemState(currentItem)

                val provider = CompressionProviderRegistry.getActiveProvider(appContext)
                val config = repository.configFlow.value

                if (isStopped) break

                val providerResult = provider.compress(appContext, inputFile, outputFile, config)
                val durationMs = System.currentTimeMillis() - startTime

                if (isStopped) break

                if (providerResult.isSuccess) {
                    val outF = providerResult.outputFile
                    val isNotBeneficial = providerResult.isNotBeneficial || outF == null || !outF.exists() || outF.length() >= inputFile.length()

                    // State pipeline: VALIDATING
                    currentItem = currentItem.copy(status = ItemStatus.VALIDATING, progress = 80)
                    repository.updateItemState(currentItem)

                    if (isNotBeneficial) {
                        val origSize = inputFile.length()
                        val completedItem = currentItem.copy(
                            status = ItemStatus.COMPLETED,
                            progress = 100,
                            compressedSize = origSize,
                            compressedUri = inputFile.absolutePath,
                            compressedWidth = item.width,
                            compressedHeight = item.height,
                            compressedFormat = item.mimeType,
                            psnrDb = null,
                            ssimScore = null,
                            isNotBeneficial = true,
                            detectedCategory = providerResult.detectedCategory,
                            targetSizeAchieved = providerResult.targetSizeAchieved,
                            processingTimeMs = providerResult.processingTimeMs
                        )

                        repository.updateItemState(completedItem)

                        HistoryLogger.logExecutionFinish(
                            appContext,
                            logId,
                            "SUCCESS",
                            "Completed: Output size was not smaller; original retained.",
                            durationMs
                        )
                    } else {
                        val outSize = outF.length()
                        val origSize = inputFile.length()

                        val metrics = ImageQualityValidator.evaluateQuality(inputFile, outF)

                        // State pipeline: SAVING_TO_GALLERY
                        currentItem = currentItem.copy(status = ItemStatus.SAVING_TO_GALLERY, progress = 90)
                        repository.updateItemState(currentItem)

                        // Automatic Gallery Save with Retry
                        val mime = providerResult.outputFormat.let {
                            when (it.lowercase()) {
                                "png" -> "image/png"
                                "webp" -> "image/webp"
                                "avif" -> "image/avif"
                                else -> "image/jpeg"
                            }
                        }

                        val gallerySaveRes = GallerySaveManager.saveToGalleryWithRetry(
                            context = appContext,
                            sourceFile = outF,
                            originalFileName = item.fileName,
                            mimeType = mime,
                            alreadySavedPath = item.savedToGalleryPath
                        )

                        val finalStatus = if (gallerySaveRes.isSuccess) ItemStatus.SAVED else ItemStatus.COMPLETED
                        val savedPath = gallerySaveRes.displayPath ?: item.savedToGalleryPath

                        val completedItem = currentItem.copy(
                            status = finalStatus,
                            progress = 100,
                            compressedSize = outSize,
                            compressedUri = outF.absolutePath,
                            compressedWidth = providerResult.outputWidth,
                            compressedHeight = providerResult.outputHeight,
                            compressedFormat = providerResult.outputFormat,
                            psnrDb = metrics.psnrDb,
                            ssimScore = metrics.ssim,
                            isNotBeneficial = false,
                            detectedCategory = providerResult.detectedCategory,
                            targetSizeAchieved = providerResult.targetSizeAchieved,
                            processingTimeMs = providerResult.processingTimeMs,
                            savedToGalleryPath = savedPath
                        )

                        repository.updateItemState(completedItem)

                        val savingsPct = if (origSize > 0) ((origSize - outSize).toDouble() / origSize.toDouble()) * 100 else 0.0
                        val summary = "Compressed ${item.fileName} (${String.format(java.util.Locale.US, "%.1f", savingsPct)}% saved)"

                        HistoryLogger.logExecutionFinish(
                            appContext,
                            logId,
                            "SUCCESS",
                            summary,
                            durationMs
                        )
                    }
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

            // Cleanup orphaned temp files older than 24h
            TempFileManager.cleanOrphanedTempFiles(appContext, activePaths)

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
        val openAppIntent = Intent(appContext, ImageCompressorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val openAppPendingIntent = PendingIntent.getActivity(appContext, 0, openAppIntent, flags)

        val cancelIntent = Intent(appContext, CancelWorkerReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(appContext, 1, cancelIntent, flags)

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Aquatic Reminez — Image Compressor")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_media)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_close, "Cancel", cancelPendingIntent)
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
            detectedCategory = e.detectedCategory,
            targetSizeAchieved = e.targetSizeAchieved,
            processingTimeMs = e.processingTimeMs,
            savedToGalleryPath = e.savedToGalleryPath,
            localTempPath = e.localTempPath,
            addedTimestamp = e.addedTimestamp
        )
    }
}
