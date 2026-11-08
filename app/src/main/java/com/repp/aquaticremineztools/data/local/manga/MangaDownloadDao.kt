package com.repp.aquaticremineztools.data.local.manga

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MangaDownloadDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MangaDownloadEntity): Long

    @Update
    suspend fun update(entity: MangaDownloadEntity)

    @Query("SELECT * FROM manga_downloads WHERE id = :id")
    suspend fun getById(id: Long): MangaDownloadEntity?

    @Query("SELECT * FROM manga_downloads WHERE chapterUrl = :rawUrl OR chapterUrl = :urlWithoutSlash OR chapterUrl = :urlWithSlash LIMIT 1")
    suspend fun getByUrl(rawUrl: String, urlWithoutSlash: String, urlWithSlash: String): MangaDownloadEntity?

    @Query("SELECT * FROM manga_downloads ORDER BY id ASC")
    fun getAllLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'FAILED') ORDER BY id ASC")
    fun getActiveAndQueuedLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status IN ('COMPLETED', 'COMPLETED_WITH_WARNING') ORDER BY timestamp DESC")
    fun getCompletedDownloadsLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun getNextQueuedItem(): MangaDownloadEntity?

    @Query("UPDATE manga_downloads SET status = 'DOWNLOADING', lastHeartbeatAt = :now WHERE id = :id AND status = 'QUEUED'")
    suspend fun claimQueuedItem(id: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE manga_downloads SET lastHeartbeatAt = :now WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun updateHeartbeat(id: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE manga_downloads SET status = 'QUEUED', lastHeartbeatAt = 0 WHERE status = 'DOWNLOADING' AND (lastHeartbeatAt = 0 OR lastHeartbeatAt < :stuckThreshold)")
    suspend fun resetStuckDownloadingItems(stuckThreshold: Long): Int

    @Query("UPDATE manga_downloads SET status = 'QUEUED', lastHeartbeatAt = 0 WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun resetItemToQueued(id: Long): Int

    @Query("UPDATE manga_downloads SET totalPages = :totalPages, downloadedPages = :downloadedPages, downloadedBytes = :bytes, totalBytes = :totalBytes, downloadSpeedBps = :speed, lastHeartbeatAt = :now WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun updateProgressIfDownloading(
        id: Long,
        totalPages: Int,
        downloadedPages: Int,
        bytes: Long,
        totalBytes: Long,
        speed: Long,
        now: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE manga_downloads SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun markFailedIfDownloading(id: Long, errorMessage: String): Int

    @Query("UPDATE manga_downloads SET status = :finalStatus, totalPages = :totalPages, downloadedPages = :downloadedPages, downloadedBytes = :bytes, totalBytes = :totalBytes, pdfPath = :pdfPath, errorMessage = :warningMessage WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun markCompletedIfDownloading(
        id: Long,
        finalStatus: String,
        totalPages: Int,
        downloadedPages: Int,
        bytes: Long,
        totalBytes: Long,
        pdfPath: String,
        warningMessage: String?
    ): Int

    @Query("SELECT COUNT(*) FROM manga_downloads WHERE (chapterUrl = :rawUrl OR chapterUrl = :urlWithoutSlash OR chapterUrl = :urlWithSlash) AND status IN ('QUEUED', 'DOWNLOADING', 'COMPLETED', 'COMPLETED_WITH_WARNING')")
    suspend fun countExistingActiveOrCompleted(rawUrl: String, urlWithoutSlash: String, urlWithSlash: String): Int

    @Query("UPDATE manga_downloads SET status = 'CANCELLED' WHERE id = :id AND status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun cancelDownload(id: Long): Int

    @Query("DELETE FROM manga_downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM manga_downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    fun getActiveAndQueuedCountLiveData(): LiveData<Int>
}
