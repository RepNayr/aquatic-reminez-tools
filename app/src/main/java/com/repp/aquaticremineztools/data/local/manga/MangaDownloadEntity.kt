package com.repp.aquaticremineztools.data.local.manga

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_downloads",
    indices = [Index(value = ["chapterUrl"], unique = false)]
)
data class MangaDownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seriesTitle: String,
    val chapterTitle: String,
    val chapterUrl: String,
    val outputFileName: String,
    val totalPages: Int,
    val downloadedPages: Int = 0,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val status: String = STATUS_QUEUED,
    val timestamp: Long = System.currentTimeMillis(),
    val lastHeartbeatAt: Long = 0L,
    val pdfPath: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_COMPLETED_WITH_WARNING = "COMPLETED_WITH_WARNING"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}
