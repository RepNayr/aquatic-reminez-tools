package com.repp.aquaticremineztools.data.local.video

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface VideoDownloadDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: VideoDownloadEntity): Long

    @Update
    suspend fun update(entity: VideoDownloadEntity)

    @Query("SELECT * FROM video_downloads WHERE id = :id")
    suspend fun getById(id: Long): VideoDownloadEntity?

    @Query("SELECT * FROM video_downloads WHERE episodeUrl = :episodeUrl AND status IN ('QUEUED', 'DOWNLOADING') LIMIT 1")
    suspend fun getActiveByEpisodeUrl(episodeUrl: String): VideoDownloadEntity?

    @Query("SELECT * FROM video_downloads ORDER BY id ASC")
    fun getAllLiveData(): LiveData<List<VideoDownloadEntity>>

    @Query("SELECT * FROM video_downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'FAILED') ORDER BY id ASC")
    fun getActiveAndQueuedLiveData(): LiveData<List<VideoDownloadEntity>>

    @Query("SELECT * FROM video_downloads WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun getNextQueuedItem(): VideoDownloadEntity?

    @Query("UPDATE video_downloads SET status = 'DOWNLOADING', lastHeartbeatAt = :now WHERE id = :id AND status = 'QUEUED'")
    suspend fun claimQueuedItem(id: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE video_downloads SET lastHeartbeatAt = :now WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun updateHeartbeat(id: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE video_downloads SET status = 'QUEUED', lastHeartbeatAt = 0 WHERE status = 'DOWNLOADING' AND (lastHeartbeatAt = 0 OR lastHeartbeatAt < :stuckThreshold)")
    suspend fun resetStuckDownloadingItems(stuckThreshold: Long): Int

    @Query("UPDATE video_downloads SET status = 'QUEUED', lastHeartbeatAt = 0 WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun resetItemToQueued(id: Long): Int

    @Query("UPDATE video_downloads SET downloadedBytes = :bytes, totalBytes = :totalBytes, downloadSpeedBps = :speed, progressPercent = :percent, lastHeartbeatAt = :now WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun updateProgressIfDownloading(
        id: Long,
        bytes: Long,
        totalBytes: Long,
        speed: Long,
        percent: Int,
        now: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE video_downloads SET status = :finalStatus, downloadedBytes = :bytes, totalBytes = :totalBytes, progressPercent = 100, videoPath = :videoPath, errorMessage = :warningMessage WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun markCompletedIfDownloading(
        id: Long,
        finalStatus: String = VideoDownloadEntity.STATUS_COMPLETED,
        bytes: Long,
        totalBytes: Long,
        videoPath: String,
        warningMessage: String? = null
    ): Int

    @Query("UPDATE video_downloads SET status = 'FAILED', errorMessage = :message WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun markFailedIfDownloading(id: Long, message: String): Int

    @Query("UPDATE video_downloads SET status = 'CANCELLED' WHERE id = :id")
    suspend fun cancelDownload(id: Long): Int

    @Query("DELETE FROM video_downloads WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
