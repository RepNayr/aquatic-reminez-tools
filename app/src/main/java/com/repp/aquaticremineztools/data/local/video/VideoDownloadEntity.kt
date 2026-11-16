package com.repp.aquaticremineztools.data.local.video

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_downloads")
data class VideoDownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seriesTitle: String,
    val episodeTitle: String,
    val episodeUrl: String,
    val downloadPageUrl: String,
    val serverId: String,
    val qualityLabel: String,
    val outputFileName: String,
    val status: String = STATUS_QUEUED,
    val stage: String = STAGE_DOWNLOADING_VIDEO,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val videoPath: String? = null,
    val lastHeartbeatAt: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    companion object {
        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"

        const val STAGE_QUEUE_CREATED = "QUEUE_CREATED"
        const val STAGE_WORKER_STARTED = "WORKER_STARTED"
        const val STAGE_ITEM_CLAIMED = "ITEM_CLAIMED"
        const val STAGE_RESOLVING_DOWNLOAD_PAGE = "RESOLVING_DOWNLOAD_PAGE"
        const val STAGE_FETCHING_GO_ENDPOINT = "FETCHING_GO_ENDPOINT"
        const val STAGE_PROCESSING_REDIRECT = "PROCESSING_REDIRECT"
        const val STAGE_MEDIA_URL_RESOLVED = "MEDIA_URL_RESOLVED"
        const val STAGE_OPENING_MEDIA_REQUEST = "OPENING_MEDIA_REQUEST"
        const val STAGE_VALIDATING_HTTP_RESPONSE = "VALIDATING_HTTP_RESPONSE"
        const val STAGE_STREAMING_STARTED = "STREAMING_STARTED"
        const val STAGE_STREAMING_COMPLETED = "STREAMING_COMPLETED"
        const val STAGE_CONVERTING_VIDEO = "CONVERTING_VIDEO"
        const val STAGE_MEDIA_VALIDATION = "MEDIA_VALIDATION"
        const val STAGE_RESULT_SAVED = "RESULT_SAVED"

        const val STAGE_DOWNLOADING_VIDEO = "DOWNLOADING_VIDEO"
        const val STAGE_COMPRESSING_VIDEO = "COMPRESSING_VIDEO"
    }
}
