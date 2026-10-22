package com.repp.aquaticremineztools.data.local.calendar

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CalendarDao {

    @Query("SELECT * FROM calendar_month_cache WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getMonthCache(monthKey: String): CalendarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthCache(entity: CalendarEntity)

    @Query("DELETE FROM calendar_month_cache WHERE updatedAt < :cutoffTime")
    suspend fun deleteOldCache(cutoffTime: Long)
}
