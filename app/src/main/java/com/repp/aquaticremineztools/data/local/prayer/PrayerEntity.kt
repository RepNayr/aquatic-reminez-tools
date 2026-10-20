package com.repp.aquaticremineztools.data.local.prayer

import androidx.room.Entity

@Entity(
    tableName = "prayer_schedules",
    primaryKeys = ["dateKey", "cityKey"]
)
data class PrayerEntity(
    val dateKey: String,
    val cityKey: String,
    val latitude: Double,
    val longitude: Double,
    val imsak: String,
    val subuh: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String,
    val updatedAt: Long
)
