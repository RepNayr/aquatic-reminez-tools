package com.repp.aquaticremineztools.data.local.video

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_results")
data class VideoResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seriesTitle: String,
    val episodeTitle: String,
    val episodeUrl: String,
    val videoPath: String,
    val fileFormat: String = "MP4",
    val resolutionLabel: String = "720p",
    val sizeBytes: Long = 0L,
    val originalSizeBytes: Long = 0L,
    val durationFormatted: String = "",
    val thumbnailUrl: String = "",
    val status: String = STATUS_COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    companion object {
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_COMPRESSING = "COMPRESSING"
        const val STATUS_FAILED = "FAILED"
    }
}
