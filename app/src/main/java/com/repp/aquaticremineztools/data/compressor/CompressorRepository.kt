package com.repp.aquaticremineztools.data.compressor

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.repp.aquaticremineztools.core.history.HistoryLogger
import com.repp.aquaticremineztools.data.local.compressor.CompressorDatabase
import com.repp.aquaticremineztools.data.local.compressor.CompressorItemEntity
import com.repp.aquaticremineztools.domain.compressor.GallerySaveManager
import com.repp.aquaticremineztools.domain.compressor.ImageHashManager
import com.repp.aquaticremineztools.domain.compressor.ImageImportManager
import com.repp.aquaticremineztools.domain.compressor.worker.CompressionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CompressorRepository private constructor(private val context: Context) {

    private val dao = CompressorDatabase.getInstance(context).compressorDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _itemsFlow = MutableStateFlow<List<CompressorItem>>(emptyList())
    val itemsFlow: StateFlow<List<CompressorItem>> = _itemsFlow.asStateFlow()

    private val _configFlow = MutableStateFlow(CompressionConfig())
    val configFlow: StateFlow<CompressionConfig> = _configFlow.asStateFlow()

    private val _isProcessingFlow = MutableStateFlow(false)
    val isProcessingFlow: StateFlow<Boolean> = _isProcessingFlow.asStateFlow()

    private val _isPausedFlow = MutableStateFlow(false)
    val isPausedFlow: StateFlow<Boolean> = _isPausedFlow.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: CompressorRepository? = null

        fun getInstance(context: Context): CompressorRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CompressorRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        scope.launch {
            // Reconcile interrupted items on startup (manual recovery check)
            val interrupted = dao.getPendingOrInterruptedItems()
            for (entity in interrupted) {
                if (entity.status in listOf(
                        ItemStatus.PROCESSING.name,
                        ItemStatus.PREPARING.name,
                        ItemStatus.VALIDATING.name,
                        ItemStatus.SAVING_TO_GALLERY.name
                    )
                ) {
                    dao.update(entity.copy(status = ItemStatus.READY.name, progress = 0))
                }
            }

            dao.getAllItemsFlow().collect { entities ->
                _itemsFlow.value = entities.map { mapEntityToItem(it) }
            }
        }
    }

    fun updateConfig(config: CompressionConfig) {
        _configFlow.value = config
    }

    suspend fun addImportedItem(item: CompressorItem, tempFile: File): String? = withContext(Dispatchers.IO) {
        val current = _itemsFlow.value
        if (current.size >= ImageImportManager.MAX_FILES_PER_BATCH) {
            return@withContext "Maximum limit of 10 images per batch reached."
        }

        val currentTotalSize = current.sumOf { it.fileSize }
        if (currentTotalSize + item.fileSize > ImageImportManager.MAX_BATCH_SIZE_BYTES) {
            return@withContext "Adding this image exceeds the 200 MB combined batch size limit."
        }

        // Compute SHA-256 hash asynchronously
        val sha256 = ImageHashManager.computeSha256(tempFile)
        if (sha256 == null) {
            tempFile.delete()
            return@withContext "Failed to calculate SHA-256 hash verification."
        }

        // Check duplicate within active session
        val isDuplicate = ImageHashManager.registerHashAndCheckDuplicate(item.id, sha256)
        if (isDuplicate) {
            tempFile.delete()
            return@withContext "Duplicate image detected. Byte-identical file already exists in session."
        }

        val updatedItem = item.copy(
            sha256 = sha256,
            status = ItemStatus.READY,
            queuePosition = current.size
        )

        dao.insert(mapItemToEntity(updatedItem))
        return@withContext null
    }

    suspend fun removeItem(itemId: String) = withContext(Dispatchers.IO) {
        val item = dao.getItemById(itemId)
        if (item != null) {
            ImageHashManager.unregisterHash(itemId)
            dao.deleteById(itemId)

            // Delete temporary output or input file if any
            item.compressedUri?.let { path ->
                try {
                    val f = File(path)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun clearQueue() = withContext(Dispatchers.IO) {
        val all = dao.getAllItemsList()
        for (item in all) {
            ImageHashManager.unregisterHash(item.id)
            item.compressedUri?.let { path ->
                try {
                    val f = File(path)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        dao.deleteAll()
        ImageImportManager.clearTempDir(context)
    }

    fun startProcessing() {
        if (_isProcessingFlow.value) return
        _isProcessingFlow.value = true
        _isPausedFlow.value = false

        scope.launch {
            // Update all READY items to QUEUED
            val all = dao.getAllItemsList()
            for (item in all) {
                if (item.status == ItemStatus.READY.name) {
                    dao.update(item.copy(status = ItemStatus.QUEUED.name))
                }
            }
            enqueueWorker()
        }
    }

    fun pauseProcessing() {
        _isPausedFlow.value = true
        _isProcessingFlow.value = false
        WorkManager.getInstance(context).cancelUniqueWork(CompressionWorker.WORK_NAME)
    }

    fun resumeProcessing() {
        if (_isProcessingFlow.value) return
        _isProcessingFlow.value = true
        _isPausedFlow.value = false
        enqueueWorker()
    }

    fun cancelTask(itemId: String) {
        scope.launch {
            val item = dao.getItemById(itemId)
            if (item != null) {
                dao.update(item.copy(status = ItemStatus.CANCELLED.name, progress = 0))
                HistoryLogger.logOpened(context, "media_image_compressor", "Image Compressor", "Media")
            }
        }
    }

    fun cancelEntireQueue() {
        _isProcessingFlow.value = false
        _isPausedFlow.value = false
        WorkManager.getInstance(context).cancelUniqueWork(CompressionWorker.WORK_NAME)

        scope.launch {
            val all = dao.getAllItemsList()
            for (item in all) {
                if (item.status in listOf(
                        ItemStatus.QUEUED.name,
                        ItemStatus.UPLOADING.name,
                        ItemStatus.PROCESSING.name,
                        ItemStatus.DOWNLOADING.name,
                        ItemStatus.VALIDATING.name
                    )
                ) {
                    dao.update(item.copy(status = ItemStatus.CANCELLED.name))
                }
            }
        }
    }

    fun retryFailed() {
        scope.launch {
            val all = dao.getAllItemsList()
            for (item in all) {
                if (item.status == ItemStatus.FAILED.name) {
                    dao.update(item.copy(status = ItemStatus.QUEUED.name, errorMessage = null, progress = 0))
                }
            }
            startProcessing()
        }
    }

    suspend fun saveIndividual(itemId: String): GallerySaveManager.SaveResult = withContext(Dispatchers.IO) {
        val entity = dao.getItemById(itemId)
            ?: return@withContext GallerySaveManager.SaveResult(false, errorMessage = "Item not found.")

        val path = entity.compressedUri ?: entity.uri
        val file = File(path)

        val result = GallerySaveManager.saveToGallery(
            context = context,
            sourceFile = file,
            originalFileName = entity.fileName,
            mimeType = entity.mimeType
        )

        if (result.isSuccess) {
            val updated = entity.copy(
                status = ItemStatus.SAVED.name,
                savedToGalleryPath = result.displayPath
            )
            dao.update(updated)

            // Log save history
            val summary = "Saved compressed file (${entity.fileName}) to ${result.displayPath}"
            HistoryLogger.logOpened(context, "media_image_compressor", "Image Compressor", "Media")
        }

        result
    }

    suspend fun saveAllResults(): List<GallerySaveManager.SaveResult> = withContext(Dispatchers.IO) {
        val completed = dao.getAllItemsList().filter { it.status == ItemStatus.COMPLETED.name }
        val results = mutableListOf<GallerySaveManager.SaveResult>()
        for (item in completed) {
            val res = saveIndividual(item.id)
            results.add(res)
        }
        results
    }

    suspend fun updateItemState(item: CompressorItem) = withContext(Dispatchers.IO) {
        dao.update(mapItemToEntity(item))
    }

    fun onWorkerCompleted() {
        _isProcessingFlow.value = false
        _isPausedFlow.value = false
    }

    private fun enqueueWorker() {
        val netPref = CompressorPreferences.getNetworkPreference(context)
        val constraints = Constraints.Builder().apply {
            if (netPref == com.repp.aquaticremineztools.data.compressor.NetworkPreference.WIFI_ONLY) {
                setRequiredNetworkType(NetworkType.UNMETERED)
            } else {
                setRequiredNetworkType(NetworkType.CONNECTED)
            }
        }.build()

        val request = OneTimeWorkRequestBuilder<CompressionWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            CompressionWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun mapEntityToItem(e: CompressorItemEntity): CompressorItem {
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

    private fun mapItemToEntity(i: CompressorItem): CompressorItemEntity {
        return CompressorItemEntity(
            id = i.id,
            uri = i.uri,
            fileName = i.fileName,
            fileSize = i.fileSize,
            mimeType = i.mimeType,
            width = i.width,
            height = i.height,
            sha256 = i.sha256,
            status = i.status.name,
            queuePosition = i.queuePosition,
            progress = i.progress,
            errorMessage = i.errorMessage,
            originalSize = i.originalSize,
            compressedSize = i.compressedSize,
            compressedUri = i.compressedUri,
            compressedWidth = i.compressedWidth,
            compressedHeight = i.compressedHeight,
            compressedFormat = i.compressedFormat,
            psnrDb = i.psnrDb,
            ssimScore = i.ssimScore,
            isNotBeneficial = i.isNotBeneficial,
            detectedCategory = i.detectedCategory,
            targetSizeAchieved = i.targetSizeAchieved,
            processingTimeMs = i.processingTimeMs,
            retryCount = 0,
            errorCategory = null,
            savedToGalleryPath = i.savedToGalleryPath,
            localTempPath = i.localTempPath,
            addedTimestamp = i.addedTimestamp
        )
    }
}
