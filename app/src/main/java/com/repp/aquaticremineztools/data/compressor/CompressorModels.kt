package com.repp.aquaticremineztools.data.compressor

enum class ItemStatus {
    CHECKING,
    READY,
    QUEUED,
    UPLOADING,
    PROCESSING,
    DOWNLOADING,
    VALIDATING,
    COMPLETED,
    FAILED,
    CANCELLED,
    SAVED
}

enum class CompressionMode {
    AUTO,
    QUALITY,
    TARGET_SIZE,
    PRESET
}

enum class CompressionPreset(val initialQuality: Int, val displayName: String) {
    LOW(90, "Low (90%)"),
    MEDIUM(75, "Medium (75%)"),
    HIGH(55, "High (55%)"),
    MAXIMUM(35, "Maximum (35%)")
}

enum class OutputFormat(val extension: String, val mimeType: String, val displayName: String) {
    ORIGINAL("orig", "image/*", "Preserve Original"),
    JPEG("jpg", "image/jpeg", "JPEG (.jpg)"),
    PNG("png", "image/png", "PNG (.png)"),
    WEBP("webp", "image/webp", "WebP (.webp)")
}

enum class ResizeMode {
    NONE,
    PERCENTAGE,
    PRESET_DIMENSIONS,
    CUSTOM
}

enum class CloudConsentState {
    NOT_SET,
    GRANTED,
    DENIED
}

enum class NetworkPreference {
    WIFI_ONLY,
    ANY_NETWORK
}

enum class ProviderStatus {
    AVAILABLE,
    LIMITED,
    UNAVAILABLE,
    BLOCKED_PRIVACY
}

data class CompressorItem(
    val id: String,
    val uri: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val sha256: String? = null,
    val status: ItemStatus = ItemStatus.CHECKING,
    val queuePosition: Int = 0,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val originalSize: Long = fileSize,
    val compressedSize: Long = 0L,
    val compressedUri: String? = null,
    val compressedWidth: Int = 0,
    val compressedHeight: Int = 0,
    val compressedFormat: String? = null,
    val psnrDb: Double? = null,
    val ssimScore: Double? = null,
    val isNotBeneficial: Boolean = false,
    val savedToGalleryPath: String? = null,
    val localTempPath: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class CompressionConfig(
    val mode: CompressionMode = CompressionMode.AUTO,
    val quality: Int = 75,
    val targetSizeBytes: Long = 500 * 1024L,
    val preset: CompressionPreset = CompressionPreset.MEDIUM,
    val outputFormat: OutputFormat = OutputFormat.ORIGINAL,
    val enableResize: Boolean = false,
    val resizeMode: ResizeMode = ResizeMode.NONE,
    val resizePercent: Int = 80,
    val customWidth: Int = 1080,
    val customHeight: Int = 1080,
    val lockAspectRatio: Boolean = true,
    val stripGps: Boolean = true,
    val preserveIcc: Boolean = true
)

data class BatchSummary(
    val totalCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalOriginalSize: Long = 0L,
    val totalCompressedSize: Long = 0L,
    val totalSavedBytes: Long = 0L,
    val savingPercentage: Double = 0.0
)
