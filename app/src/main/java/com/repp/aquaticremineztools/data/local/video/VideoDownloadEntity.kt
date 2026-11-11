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

        const val STAGE_DOWNLOADING_VIDEO = "DOWNLOADING_VIDEO"
        const val STAGE_COMPRESSING_VIDEO = "COMPRESSING_VIDEO"
    }
}
