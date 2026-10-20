package com.repp.aquaticremineztools.data.local.prayer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_schedules WHERE dateKey = :dateKey AND cityKey = :cityKey LIMIT 1")
    suspend fun getSchedule(dateKey: String, cityKey: String): PrayerEntity?

    @Query("SELECT * FROM prayer_schedules WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getScheduleByDate(dateKey: String): PrayerEntity?

    @Query("SELECT * FROM prayer_schedules ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestSchedule(): PrayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(entity: PrayerEntity)

    @Query("DELETE FROM prayer_schedules WHERE updatedAt < :cutoffTime")
    suspend fun deleteOldSchedules(cutoffTime: Long)
}
