package com.repp.aquaticremineztools.data.model

data class CalendarDay(
    val dayOfMonth: Int,
    val gregorianDateKey: String,
    val fullGregorianFormatted: String,
    val hijriDay: String,
    val hijriMonthNumber: Int,
    val hijriMonthName: String,
    val hijriYear: String,
    val fullHijriFormatted: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isInCurrentMonth: Boolean,
    val isSunday: Boolean,
    val isFriday: Boolean,
    val javanesePasaran: String,
    val observance: String? = null
)
