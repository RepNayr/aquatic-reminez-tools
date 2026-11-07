package com.repp.aquaticremineztools.data.local.manga

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MangaDownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MangaDownloadEntity): Long

    @Update
    suspend fun update(entity: MangaDownloadEntity)

    @Query("SELECT * FROM manga_downloads WHERE id = :id")
    suspend fun getById(id: Long): MangaDownloadEntity?

    @Query("SELECT * FROM manga_downloads WHERE chapterUrl = :chapterUrl OR chapterUrl = :normalizedUrl LIMIT 1")
    suspend fun getByUrl(chapterUrl: String, normalizedUrl: String): MangaDownloadEntity?

    @Query("SELECT * FROM manga_downloads ORDER BY id DESC")
    fun getAllLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status IN ('QUEUED', 'DOWNLOADING') ORDER BY id ASC")
    fun getActiveAndQueuedLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status IN ('COMPLETED', 'COMPLETED_WITH_WARNING') ORDER BY timestamp DESC")
    fun getCompletedDownloadsLiveData(): LiveData<List<MangaDownloadEntity>>

    @Query("SELECT * FROM manga_downloads WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun getNextQueuedItem(): MangaDownloadEntity?

    @Query("SELECT * FROM manga_downloads WHERE status = 'DOWNLOADING' LIMIT 1")
    suspend fun getActiveDownloadingItem(): MangaDownloadEntity?

    @Query("UPDATE manga_downloads SET status = 'DOWNLOADING' WHERE id = :id AND status = 'QUEUED'")
    suspend fun claimQueuedItem(id: Long): Int

    @Query("UPDATE manga_downloads SET status = 'QUEUED' WHERE status = 'DOWNLOADING'")
    suspend fun resetStuckDownloadingItems(): Int

    @Query("SELECT COUNT(*) FROM manga_downloads WHERE (chapterUrl = :chapterUrl OR chapterUrl = :normalizedUrl) AND status IN ('QUEUED', 'DOWNLOADING', 'COMPLETED', 'COMPLETED_WITH_WARNING')")
    suspend fun countExistingChapter(chapterUrl: String, normalizedUrl: String): Int

    @Query("UPDATE manga_downloads SET status = 'CANCELLED' WHERE id = :id AND status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun cancelDownload(id: Long): Int

    @Query("DELETE FROM manga_downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM manga_downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    fun getActiveAndQueuedCountLiveData(): LiveData<Int>
}
