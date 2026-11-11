package com.repp.aquaticremineztools.data.local.video

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface VideoResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VideoResultEntity): Long

    @Update
    suspend fun update(entity: VideoResultEntity)

    @Query("SELECT * FROM video_results WHERE id = :id")
    suspend fun getById(id: Long): VideoResultEntity?

    @Query("SELECT * FROM video_results WHERE episodeUrl = :episodeUrl LIMIT 1")
    suspend fun getByEpisodeUrl(episodeUrl: String): VideoResultEntity?

    @Query("SELECT * FROM video_results ORDER BY timestamp DESC")
    fun getAllLiveData(): LiveData<List<VideoResultEntity>>

    @Query("UPDATE video_results SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null): Int

    @Query("UPDATE video_results SET videoPath = :videoPath, sizeBytes = :sizeBytes, originalSizeBytes = :oldSizeBytes, status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateVideoResult(
        id: Long,
        videoPath: String,
        sizeBytes: Long,
        oldSizeBytes: Long,
        status: String = VideoResultEntity.STATUS_COMPLETED,
        errorMessage: String? = null
    ): Int

    @Query("DELETE FROM video_results WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
