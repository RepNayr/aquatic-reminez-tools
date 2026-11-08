package com.repp.aquaticremineztools.data.local.manga

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MangaResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MangaResultEntity): Long

    @Update
    suspend fun update(entity: MangaResultEntity)

    @Query("SELECT * FROM manga_results WHERE id = :id")
    suspend fun getById(id: Long): MangaResultEntity?

    @Query("SELECT * FROM manga_results ORDER BY timestamp DESC")
    fun getAllResultsLiveData(): LiveData<List<MangaResultEntity>>

    @Query("UPDATE manga_results SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)

    @Query("UPDATE manga_results SET pdfPath = :pdfPath, sizeBytes = :sizeBytes, status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updatePdfResult(id: Long, pdfPath: String, sizeBytes: Long, status: String, errorMessage: String? = null)

    @Query("DELETE FROM manga_results WHERE id = :id")
    suspend fun deleteById(id: Long)
}
