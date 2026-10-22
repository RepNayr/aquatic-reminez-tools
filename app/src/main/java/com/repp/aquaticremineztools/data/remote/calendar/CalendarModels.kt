package com.repp.aquaticremineztools.data.remote.calendar

import com.google.gson.annotations.SerializedName

data class AladhanCalendarResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<AladhanCalendarDayData>?
)

data class AladhanCalendarDayData(
    @SerializedName("gregorian") val gregorian: AladhanGregorianDateInfo?,
    @SerializedName("hijri") val hijri: AladhanHijriDateInfo?
)

data class AladhanGregorianDateInfo(
    @SerializedName("date") val date: String?,
    @SerializedName("day") val day: String?,
    @SerializedName("weekday") val weekday: AladhanWeekdayInfo?,
    @SerializedName("month") val month: AladhanMonthNumberInfo?,
    @SerializedName("year") val year: String?
)

data class AladhanHijriDateInfo(
    @SerializedName("date") val date: String?,
    @SerializedName("day") val day: String?,
    @SerializedName("weekday") val weekday: AladhanWeekdayInfo?,
    @SerializedName("month") val month: AladhanHijriMonthNumberInfo?,
    @SerializedName("year") val year: String?,
    @SerializedName("holidays") val holidays: List<String>?
)

data class AladhanWeekdayInfo(
    @SerializedName("en") val en: String?,
    @SerializedName("ar") val ar: String?
)

data class AladhanMonthNumberInfo(
    @SerializedName("number") val number: Int?,
    @SerializedName("en") val en: String?
)

data class AladhanHijriMonthNumberInfo(
    @SerializedName("number") val number: Int?,
    @SerializedName("en") val en: String?,
    @SerializedName("ar") val ar: String?
)
