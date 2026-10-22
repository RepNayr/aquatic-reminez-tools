package com.repp.aquaticremineztools.core.util

import java.util.Calendar

data class HijriCalcResult(
    val day: Int,
    val monthNumber: Int,
    val monthName: String,
    val year: String
)

object HijriCalendarHelper {

    fun getIndonesianHijriMonthName(monthNumber: Int): String {
        return when (monthNumber) {
            1 -> "Muharram"
            2 -> "Safar"
            3 -> "Rabiul Awal"
            4 -> "Rabiul Akhir"
            5 -> "Jumadil Awal"
            6 -> "Jumadil Akhir"
            7 -> "Rajab"
            8 -> "Sya'ban"
            9 -> "Ramadhan"
            10 -> "Syawal"
            11 -> "Dzulqa'dah"
            12 -> "Dzulhijjah"
            else -> "Muharram"
        }
    }

    fun getIndonesianDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }
    }

    fun getIndonesianGregorianMonthName(monthIndexZeroBased: Int): String {
        return when (monthIndexZeroBased) {
            0 -> "Januari"
            1 -> "Februari"
            2 -> "Maret"
            3 -> "April"
            4 -> "Mei"
            5 -> "Juni"
            6 -> "Juli"
            7 -> "Agustus"
            8 -> "September"
            9 -> "Oktober"
            10 -> "November"
            11 -> "Desember"
            else -> ""
        }
    }

    fun formatFullGregorianDate(calendar: Calendar): String {
        val dayName = getIndonesianDayName(calendar.get(Calendar.DAY_OF_WEEK))
        val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
        val monthName = getIndonesianGregorianMonthName(calendar.get(Calendar.MONTH))
        val year = calendar.get(Calendar.YEAR)
        return "$dayName, $dayNum $monthName $year"
    }

    fun convertGregorianToHijri(year: Int, month1Based: Int, day: Int): HijriCalcResult {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val localDate = java.time.LocalDate.of(year, month1Based, day)
                val hijrahDate = java.time.chrono.HijrahChronology.INSTANCE.date(localDate)
                val hDay = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
                val hMonth = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                val hYear = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
                return HijriCalcResult(
                    day = hDay,
                    monthNumber = hMonth,
                    monthName = getIndonesianHijriMonthName(hMonth),
                    year = hYear.toString()
                )
            } catch (_: Throwable) {
                // Fallback to tabular
            }
        }

        val a = (14 - month1Based) / 12
        val y = year + 4800 - a
        val m = month1Based + 12 * a - 3
        val jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        var hMonth = (24 * l) / 709
        val hDay = l - (709 * hMonth) / 24
        val hYear = 30 * n + j - 30

        if (hMonth < 1) hMonth = 1
        if (hMonth > 12) hMonth = 12

        return HijriCalcResult(
            day = hDay,
            monthNumber = hMonth,
            monthName = getIndonesianHijriMonthName(hMonth),
            year = hYear.toString()
        )
    }

    fun getVerifiedObservance(hijriDay: Int, hijriMonth: Int, hijriYear: String): String? {
        return when {
            hijriMonth == 1 && hijriDay == 1 -> "Tahun Baru Islam $hijriYear H"
            hijriMonth == 3 && hijriDay == 12 -> "Maulid Nabi Muhammad SAW"
            hijriMonth == 7 && hijriDay == 27 -> "Isra Mi'raj Nabi Muhammad SAW"
            hijriMonth == 9 && hijriDay == 1 -> "Awal Puasa Ramadhan"
            hijriMonth == 9 && hijriDay == 17 -> "Nuzulul Qur'an"
            hijriMonth == 10 && hijriDay == 1 -> "Hari Raya Idul Fitri $hijriYear H"
            hijriMonth == 10 && hijriDay == 2 -> "Hari Raya Idul Fitri (Hari Ke-2)"
            hijriMonth == 12 && hijriDay == 9 -> "Hari Arafah"
            hijriMonth == 12 && hijriDay == 10 -> "Hari Raya Idul Adha $hijriYear H"
            hijriMonth == 12 && (hijriDay in 11..13) -> "Hari Tasyrik"
            else -> null
        }
    }
}
