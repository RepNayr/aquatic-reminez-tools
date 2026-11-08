package com.repp.aquaticremineztools.data.local.manga

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga_results")
data class MangaResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seriesTitle: String,
    val chapterTitle: String,
    val chapterUrl: String,
    val pdfPath: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val status: String = STATUS_NOT_COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    companion object {
        const val STATUS_NOT_COMPLETED = "NOT_COMPLETED"
        const val STATUS_COMPRESSING = "COMPRESSING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
