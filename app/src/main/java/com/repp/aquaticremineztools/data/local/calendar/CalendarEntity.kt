package com.repp.aquaticremineztools.data.local.calendar

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_month_cache")
data class CalendarEntity(
    @PrimaryKey val monthKey: String,
    val jsonDaysData: String,
    val hijriMonthSummary: String,
    val updatedAt: Long
)
