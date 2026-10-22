package com.repp.aquaticremineztools.data.model

data class CalendarMonthData(
    val year: Int,
    val month: Int,
    val gregorianMonthTitle: String,
    val hijriMonthSubtitle: String,
    val days: List<CalendarDay>,
    val selectedDay: CalendarDay?,
    val isCached: Boolean,
    val isOnline: Boolean,
    val statusText: String
)
