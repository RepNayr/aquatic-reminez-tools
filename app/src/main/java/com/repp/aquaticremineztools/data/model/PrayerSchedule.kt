package com.repp.aquaticremineztools.data.model

data class PrayerSchedule(
    val dateKey: String, // format: "DD-MM-YYYY"
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val imsak: String,   // e.g. "04:51"
    val subuh: String,   // e.g. "05:01"
    val dzuhur: String,  // e.g. "11:56"
    val ashar: String,   // e.g. "15:18"
    val maghrib: String, // e.g. "17:55"
    val isya: String,    // e.g. "19:05"
    val isCached: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
