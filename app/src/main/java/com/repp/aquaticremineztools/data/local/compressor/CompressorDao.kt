package com.repp.aquaticremineztools.data.local.compressor

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CompressorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CompressorItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompressorItemEntity>)

    @Update
    suspend fun update(item: CompressorItemEntity)

    @Delete
    suspend fun delete(item: CompressorItemEntity)

    @Query("DELETE FROM compressor_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM compressor_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM compressor_items ORDER BY queuePosition ASC, addedTimestamp ASC")
    fun getAllItemsFlow(): Flow<List<CompressorItemEntity>>

    @Query("SELECT * FROM compressor_items ORDER BY queuePosition ASC, addedTimestamp ASC")
    suspend fun getAllItemsList(): List<CompressorItemEntity>

    @Query("SELECT * FROM compressor_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): CompressorItemEntity?

    @Query("SELECT * FROM compressor_items WHERE status IN ('QUEUED', 'UPLOADING', 'PROCESSING', 'DOWNLOADING', 'VALIDATING', 'PREPARING', 'SAVING_TO_GALLERY') ORDER BY queuePosition ASC LIMIT 1")
    suspend fun getNextEligibleItem(): CompressorItemEntity?

    @Query("SELECT * FROM compressor_items WHERE status IN ('QUEUED', 'UPLOADING', 'PROCESSING', 'DOWNLOADING', 'VALIDATING', 'PREPARING', 'SAVING_TO_GALLERY') ORDER BY queuePosition ASC")
    suspend fun getPendingOrInterruptedItems(): List<CompressorItemEntity>
}
