package com.repp.aquaticremineztools.presentation.compressor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.compressor.BatchSummary
import com.repp.aquaticremineztools.data.compressor.CompressionConfig
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import com.repp.aquaticremineztools.data.compressor.CompressorRepository
import com.repp.aquaticremineztools.data.compressor.ItemStatus
import com.repp.aquaticremineztools.domain.compressor.GallerySaveManager
import com.repp.aquaticremineztools.domain.compressor.ImageImportManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageCompressorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CompressorRepository.getInstance(application)

    val itemsFlow: StateFlow<List<CompressorItem>> = repository.itemsFlow
    val configFlow: StateFlow<CompressionConfig> = repository.configFlow
    val isProcessingFlow: StateFlow<Boolean> = repository.isProcessingFlow
    val isPausedFlow: StateFlow<Boolean> = repository.isPausedFlow

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _duplicatePulseEvent = MutableSharedFlow<String>()
    val duplicatePulseEvent: SharedFlow<String> = _duplicatePulseEvent.asSharedFlow()

    private val _batchSummaryFlow = MutableStateFlow(BatchSummary())
    val batchSummaryFlow: StateFlow<BatchSummary> = _batchSummaryFlow.asStateFlow()

    init {
        viewModelScope.launch {
            repository.itemsFlow.collect { items ->
                calculateSummary(items)
            }
        }
    }

    fun importUris(uris: List<Uri>) {
        viewModelScope.launch {
            var addedCount = 0
            var duplicateCount = 0
            var maxLimitReached = false
            var maxSizeReached = false
            val errors = mutableListOf<String>()

            for (uri in uris) {
                when (val result = ImageImportManager.importUri(getApplication(), uri)) {
                    is ImageImportManager.ImportResult.Success -> {
                        when (val addRes = repository.addImportedItem(result.item, result.tempFile)) {
                            is com.repp.aquaticremineztools.data.compressor.AddResult.Success -> {
                                addedCount++
                            }
                            is com.repp.aquaticremineztools.data.compressor.AddResult.Duplicate -> {
                                duplicateCount++
                                _duplicatePulseEvent.emit(addRes.existingItemId)
                            }
                            is com.repp.aquaticremineztools.data.compressor.AddResult.MaxCountReached -> {
                                maxLimitReached = true
                            }
                            is com.repp.aquaticremineztools.data.compressor.AddResult.MaxSizeReached -> {
                                maxSizeReached = true
                            }
                            is com.repp.aquaticremineztools.data.compressor.AddResult.Error -> {
                                errors.add("${result.item.fileName}: ${addRes.message}")
                            }
                        }
                    }
                    is ImageImportManager.ImportResult.Error -> {
                        errors.add("${result.fileName}: ${result.reason}")
                    }
                }
            }

            if (uris.size == 1) {
                if (duplicateCount == 1) {
                    _toastEvent.emit("Duplicate image skipped")
                } else if (maxLimitReached) {
                    _toastEvent.emit("Maximum 10 images limit reached")
                } else if (maxSizeReached) {
                    _toastEvent.emit("Batch size limit (200 MB) reached")
                } else if (errors.isNotEmpty()) {
                    _toastEvent.emit(errors.first())
                }
            } else {
                val parts = mutableListOf<String>()
                if (addedCount > 0) {
                    parts.add("$addedCount image${if (addedCount > 1) "s" else ""} added")
                }
                if (duplicateCount > 0) {
                    parts.add("$duplicateCount duplicate${if (duplicateCount > 1) "s" else ""} skipped")
                }
                if (maxLimitReached) {
                    parts.add("maximum 10 images reached")
                } else if (maxSizeReached) {
                    parts.add("200 MB size limit reached")
                }
                if (parts.isNotEmpty()) {
                    _toastEvent.emit(parts.joinToString(", "))
                } else if (errors.isNotEmpty()) {
                    _toastEvent.emit(errors.first())
                }
            }
        }
    }

    fun updateConfig(config: CompressionConfig) {
        repository.updateConfig(config)
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            repository.removeItem(itemId)
        }
    }

    fun startCompression() {
        val readyCount = repository.itemsFlow.value.count { it.status == ItemStatus.READY || it.status == ItemStatus.QUEUED }
        if (readyCount == 0) {
            viewModelScope.launch {
                _toastEvent.emit("No valid ready images in workspace to compress.")
            }
            return
        }
        repository.startProcessing()
    }

    fun pauseQueue() {
        repository.pauseProcessing()
    }

    fun resumeQueue() {
        repository.resumeProcessing()
    }

    fun cancelTask(itemId: String) {
        repository.cancelTask(itemId)
    }

    fun cancelEntireQueue() {
        repository.cancelEntireQueue()
    }

    fun retryFailed() {
        repository.retryFailed()
    }

    fun clearQueue() {
        viewModelScope.launch {
            repository.clearQueue()
        }
    }

    fun saveIndividual(itemId: String) {
        viewModelScope.launch {
            val result = repository.saveIndividual(itemId)
            if (result.isSuccess) {
                _toastEvent.emit("Saved to ${result.displayPath}")
            } else {
                _toastEvent.emit(result.errorMessage ?: "Failed to save image.")
            }
        }
    }

    fun saveAllResults() {
        viewModelScope.launch {
            val results = repository.saveAllResults()
            val successCount = results.count { it.isSuccess }
            _toastEvent.emit("Saved $successCount images to Pictures/Aquatic Reminez/Compressed")
        }
    }

    fun discardIndividual(itemId: String) {
        viewModelScope.launch {
            repository.removeItem(itemId)
        }
    }

    fun discardAllResults() {
        viewModelScope.launch {
            val completed = repository.itemsFlow.value.filter { it.status == ItemStatus.COMPLETED || it.status == ItemStatus.SAVED }
            for (item in completed) {
                repository.removeItem(item.id)
            }
        }
    }

    private fun calculateSummary(items: List<CompressorItem>) {
        val totalCount = items.size
        val successCount = items.count { it.status == ItemStatus.COMPLETED || it.status == ItemStatus.SAVED }
        val failedCount = items.count { it.status == ItemStatus.FAILED }
        val cancelledCount = items.count { it.status == ItemStatus.CANCELLED }

        val completedItems = items.filter { it.status == ItemStatus.COMPLETED || it.status == ItemStatus.SAVED }
        val totalOrig = completedItems.sumOf { it.originalSize }
        val totalComp = completedItems.sumOf { it.compressedSize }
        val totalSaved = (totalOrig - totalComp).coerceAtLeast(0L)

        val savingPct = if (totalOrig > 0) (totalSaved.toDouble() / totalOrig.toDouble()) * 100.0 else 0.0

        _batchSummaryFlow.value = BatchSummary(
            totalCount = totalCount,
            successCount = successCount,
            failedCount = failedCount,
            cancelledCount = cancelledCount,
            totalOriginalSize = totalOrig,
            totalCompressedSize = totalComp,
            totalSavedBytes = totalSaved,
            savingPercentage = savingPct
        )
    }
}
