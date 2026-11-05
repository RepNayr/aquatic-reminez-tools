package com.repp.aquaticremineztools.data.compressor

enum class ItemStatus {
    CHECKING,
    READY,
    QUEUED,
    PREPARING,
    UPLOADING,
    PROCESSING,
    DOWNLOADING,
    VALIDATING,
    SAVING_TO_GALLERY,
    COMPLETED,
    SAVE_FAILED,
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

enum class TargetSizePriority(val displayName: String) {
    QUALITY_FIRST("Quality First"),
    SIZE_FIRST("Size First"),
    BALANCED("Balanced")
}

enum class ResolutionPreset(val targetLongSide: Int, val displayName: String) {
    ORIGINAL(0, "Original Size"),
    RES_4K(3840, "4K (3840px)"),
    RES_2K(2560, "2K (2560px)"),
    RES_1080P(1920, "1080p (1920px)"),
    RES_720P(1280, "720p (1280px)"),
    RES_480P(854, "480p (854px)"),
    CUSTOM(-1, "Custom Size")
}

enum class CompressionPreset(val initialQuality: Int, val displayName: String, val description: String) {
    SMART_AUTO(78, "Smart Auto", "Adaptive AI analysis"),
    LIGHT(88, "Light", "Preserve high quality"),
    BALANCED(75, "Balanced", "Optimal size & quality"),
    STRONG(55, "Strong", "Aggressive compression"),
    EXTREME(35, "Extreme", "Maximum size reduction"),
    CUSTOM(75, "Custom", "User configured"),

    // Aliases for compatibility
    LOW(90, "Low (90%)", "Light optimization"),
    MEDIUM(75, "Medium (75%)", "Balanced optimization"),
    HIGH(55, "High (55%)", "Strong optimization"),
    MAXIMUM(35, "Maximum (35%)", "Maximum optimization")
}

enum class OutputFormat(val extension: String, val mimeType: String, val displayName: String) {
    SMART("smart", "image/*", "Smart Format"),
    ORIGINAL("orig", "image/*", "Preserve Original"),
    JPEG("jpg", "image/jpeg", "JPEG (.jpg)"),
    PNG("png", "image/png", "PNG (.png)"),
    WEBP("webp", "image/webp", "WebP (.webp)"),
    AVIF("avif", "image/avif", "AVIF (.avif)"),
    AUTO_BEST("auto_best", "image/*", "Auto Best Format")
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
    val detectedCategory: String? = null,
    val targetSizeAchieved: Boolean? = null,
    val processingTimeMs: Long = 0L,
    val savedToGalleryPath: String? = null,
    val localTempPath: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class CompressionConfig(
    val mode: CompressionMode = CompressionMode.AUTO,
    val quality: Int = 75,
    val minQuality: Int = 30,
    val maxQualityLoss: Int = 50,
    val targetSizeBytes: Long = 500 * 1024L,
    val targetSizePriority: TargetSizePriority = TargetSizePriority.BALANCED,
    val preset: CompressionPreset = CompressionPreset.BALANCED,
    val outputFormat: OutputFormat = OutputFormat.ORIGINAL,
    val resolutionPreset: ResolutionPreset = ResolutionPreset.ORIGINAL,
    val enableResize: Boolean = false,
    val resizeMode: ResizeMode = ResizeMode.NONE,
    val resizePercent: Int = 80,
    val customWidth: Int = 1080,
    val customHeight: Int = 1080,
    val lockAspectRatio: Boolean = true,
    val enableSharpen: Boolean = false,
    val enableDenoise: Boolean = false,
    val enablePreserveDetails: Boolean = false,
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

sealed class AddResult {
    object Success : AddResult()
    data class Duplicate(val existingItemId: String) : AddResult()
    object MaxCountReached : AddResult()
    object MaxSizeReached : AddResult()
    data class Error(val message: String) : AddResult()
}

