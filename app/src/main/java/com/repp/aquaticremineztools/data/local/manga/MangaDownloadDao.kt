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

    @Query("SELECT * FROM manga_downloads WHERE chapterUrl = :chapterUrl OR chapterUrl = :normalizedUrl LIMIT 1")
    suspend fun getByUrl(chapterUrl: String, normalizedUrl: String): MangaDownloadEntity?

    @Query("SELECT * FROM manga_downloads ORDER BY id ASC")
    fun getAllLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status IN ('QUEUED', 'DOWNLOADING') ORDER BY id ASC")
    fun getActiveAndQueuedLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status IN ('COMPLETED', 'COMPLETED_WITH_WARNING') ORDER BY timestamp DESC")
    fun getCompletedDownloadsLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun getNextQueuedItem(): MangaDownloadEntity?

    @Query("UPDATE manga_downloads SET status = 'DOWNLOADING', lastHeartbeatAt = :now WHERE id = :id AND status = 'QUEUED'")
    suspend fun claimQueuedItem(id: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE manga_downloads SET lastHeartbeatAt = :now WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun updateHeartbeat(id: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE manga_downloads SET status = 'QUEUED', lastHeartbeatAt = 0 WHERE status = 'DOWNLOADING' AND lastHeartbeatAt > 0 AND lastHeartbeatAt < :stuckThreshold")
    suspend fun resetStuckDownloadingItems(stuckThreshold: Long): Int

    @Query("UPDATE manga_downloads SET downloadedPages = :pages, downloadedBytes = :bytes, totalBytes = :totalBytes, downloadSpeedBps = :speed, lastHeartbeatAt = :now WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun updateProgressIfDownloading(id: Long, pages: Int, bytes: Long, totalBytes: Long, speed: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE manga_downloads SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun markFailedIfDownloading(id: Long, errorMessage: String): Int

    @Query("UPDATE manga_downloads SET status = :finalStatus, downloadedPages = :pages, downloadedBytes = :bytes, totalBytes = :totalBytes, pdfPath = :pdfPath, errorMessage = :warningMessage WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun markCompletedIfDownloading(id: Long, finalStatus: String, pages: Int, bytes: Long, totalBytes: Long, pdfPath: String, warningMessage: String?): Int

    @Query("SELECT COUNT(*) FROM manga_downloads WHERE (chapterUrl = :chapterUrl OR chapterUrl = :normalizedUrl) AND status IN ('QUEUED', 'DOWNLOADING', 'COMPLETED', 'COMPLETED_WITH_WARNING')")
    suspend fun countExistingActiveOrCompleted(chapterUrl: String, normalizedUrl: String): Int

    @Query("UPDATE manga_downloads SET status = 'CANCELLED' WHERE id = :id AND status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun cancelDownload(id: Long): Int

    @Query("DELETE FROM manga_downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM manga_downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    fun getActiveAndQueuedCountLiveData(): LiveData<Int>
}
