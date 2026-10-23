package com.repp.aquaticremineztools.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.core.util.HijriCalendarHelper
import com.repp.aquaticremineztools.data.local.calendar.CalendarDatabase
import com.repp.aquaticremineztools.data.local.calendar.CalendarEntity
import com.repp.aquaticremineztools.data.model.CalendarDay
import com.repp.aquaticremineztools.data.model.CalendarMonthData
import com.repp.aquaticremineztools.data.remote.calendar.CalendarApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class CalendarRepository(context: Context) {

    private val appContext = context.applicationContext
    private val calendarDao = CalendarDatabase.getInstance(appContext).calendarDao()
    private val gson = Gson()

    suspend fun getMonthCalendarData(
        year: Int,
        month1Based: Int,
        selectedDayNumber: Int?
    ): CalendarMonthData = withContext(Dispatchers.IO) {
        val isEng = SettingsPreferences.getLanguage(appContext) != "id"
        val monthKey = String.format(Locale.US, "%04d-%02d", year, month1Based)

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH) + 1
        val todayDayNum = todayCal.get(Calendar.DAY_OF_MONTH)

        val cachedEntity = calendarDao.getMonthCache(monthKey)

        var apiDaysMap: Map<Int, RawHijriData>? = null
        var isOnline = false

        try {
            val response = CalendarApiClient.service.getGregorianToHijriCalendar(
                month = month1Based,
                year = year,
                method = 20,
                adjustment = 0
            )

            if (response.isSuccessful && response.body()?.code == 200) {
                val dayDataList = response.body()?.data
                if (!dayDataList.isNullOrEmpty()) {
                    val map = mutableMapOf<Int, RawHijriData>()
                    for (item in dayDataList) {
                        val gDay = item.gregorian?.day?.toIntOrNull() ?: continue
                        val hDay = item.hijri?.day ?: "1"
                        val hMonthNum = item.hijri?.month?.number ?: 1
                        val hMonthName = HijriCalendarHelper.getIndonesianHijriMonthName(hMonthNum)
                        val hYear = item.hijri?.year ?: "1448"

                        val apiHolidays = item.hijri?.holidays
                        val holidayObs = if (!apiHolidays.isNullOrEmpty()) {
                            apiHolidays.joinToString(", ")
                        } else {
                            HijriCalendarHelper.getVerifiedObservance(
                                hijriDay = hDay.toIntOrNull() ?: 1,
                                hijriMonth = hMonthNum,
                                hijriYear = hYear
                            )
                        }

                        map[gDay] = RawHijriData(
                            hijriDay = hDay,
                            hijriMonthNumber = hMonthNum,
                            hijriMonthName = hMonthName,
                            hijriYear = hYear,
                            observance = holidayObs
                        )
                    }
                    if (map.isNotEmpty()) {
                        apiDaysMap = map
                        isOnline = true

                        // Save to cache
                        val json = gson.toJson(map)
                        calendarDao.insertMonthCache(
                            CalendarEntity(
                                monthKey = monthKey,
                                jsonDaysData = json,
                                hijriMonthSummary = extractHijriMonthSummary(map),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Network error
        }

        // Fallback to cache if network failed
        var isCached = false
        if (apiDaysMap == null && cachedEntity != null) {
            try {
                val type = object : TypeToken<Map<Int, RawHijriData>>() {}.type
                val cachedMap: Map<Int, RawHijriData>? = gson.fromJson(cachedEntity.jsonDaysData, type)
                if (!cachedMap.isNullOrEmpty()) {
                    apiDaysMap = cachedMap
                    isCached = true
                }
            } catch (_: Exception) {
            }
        }

        val statusText = when {
            isOnline -> if (isEng) "Online · Latest data" else "Online · Data terbaru"
            isCached -> if (isEng) "Offline · Saved data" else "Offline · Data tersimpan"
            else -> if (isEng) "Offline · Saved data" else "Offline · Data tersimpan"
        }

        // Build grid of 42 cells
        val daysInGrid = mutableListOf<CalendarDay>()

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month1Based - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val firstDayColumnIndex = (firstDayOfWeek + 5) % 7 // 0 = Monday, 6 = Sunday

        // Previous month info
        val prevCal = Calendar.getInstance()
        prevCal.set(Calendar.YEAR, year)
        prevCal.set(Calendar.MONTH, month1Based - 1)
        prevCal.add(Calendar.MONTH, -1)
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth1Based = prevCal.get(Calendar.MONTH) + 1
        val prevMonthMaxDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Next month info
        val nextCal = Calendar.getInstance()
        nextCal.set(Calendar.YEAR, year)
        nextCal.set(Calendar.MONTH, month1Based - 1)
        nextCal.add(Calendar.MONTH, 1)
        val nextYear = nextCal.get(Calendar.YEAR)
        val nextMonth1Based = nextCal.get(Calendar.MONTH) + 1

        var activeSelectedDay: CalendarDay? = null

        for (i in 0 until 42) {
            val isColumnSunday = (i % 7 == 6)
            val isColumnFriday = (i % 7 == 4)

            if (i < firstDayColumnIndex) {
                // Trailing days of previous month
                val dayNum = prevMonthMaxDays - (firstDayColumnIndex - 1 - i)
                val gCal = Calendar.getInstance()
                gCal.set(prevYear, prevMonth1Based - 1, dayNum)
                val fullGDate = HijriCalendarHelper.formatFullGregorianDate(gCal, isEng)
                val gDateKey = String.format(Locale.US, "%04d-%02d-%02d", prevYear, prevMonth1Based, dayNum)
                val pasaran = HijriCalendarHelper.getJavanesePasaran(prevYear, prevMonth1Based, dayNum)

                val calc = HijriCalendarHelper.convertGregorianToHijri(prevYear, prevMonth1Based, dayNum)
                val hDayStr = calc.day.toString()
                val hMonthNum = calc.monthNumber
                val hMonthName = calc.monthName
                val hYearStr = calc.year
                val fullHDate = "$hDayStr $hMonthName $hYearStr H"
                val obs = HijriCalendarHelper.getVerifiedObservance(calc.day, hMonthNum, hYearStr)
                    ?: HijriCalendarHelper.getGregorianHoliday(prevYear, prevMonth1Based, dayNum)

                val isToday = (prevYear == todayYear && prevMonth1Based == todayMonth && dayNum == todayDayNum)
                val isSelected = false

                val dayObj = CalendarDay(
                    dayOfMonth = dayNum,
                    gregorianDateKey = gDateKey,
                    fullGregorianFormatted = fullGDate,
                    hijriDay = hDayStr,
                    hijriMonthNumber = hMonthNum,
                    hijriMonthName = hMonthName,
                    hijriYear = hYearStr,
                    fullHijriFormatted = fullHDate,
                    isToday = isToday,
                    isSelected = isSelected,
                    isInCurrentMonth = false,
                    isSunday = isColumnSunday,
                    isFriday = isColumnFriday,
                    javanesePasaran = pasaran,
                    observance = obs
                )
                daysInGrid.add(dayObj)
            } else if (i >= firstDayColumnIndex + daysInMonth) {
                // Leading days of next month
                val dayNum = i - (firstDayColumnIndex + daysInMonth) + 1
                val gCal = Calendar.getInstance()
                gCal.set(nextYear, nextMonth1Based - 1, dayNum)
                val fullGDate = HijriCalendarHelper.formatFullGregorianDate(gCal, isEng)
                val gDateKey = String.format(Locale.US, "%04d-%02d-%02d", nextYear, nextMonth1Based, dayNum)
                val pasaran = HijriCalendarHelper.getJavanesePasaran(nextYear, nextMonth1Based, dayNum)

                val calc = HijriCalendarHelper.convertGregorianToHijri(nextYear, nextMonth1Based, dayNum)
                val hDayStr = calc.day.toString()
                val hMonthNum = calc.monthNumber
                val hMonthName = calc.monthName
                val hYearStr = calc.year
                val fullHDate = "$hDayStr $hMonthName $hYearStr H"
                val obs = HijriCalendarHelper.getVerifiedObservance(calc.day, hMonthNum, hYearStr)
                    ?: HijriCalendarHelper.getGregorianHoliday(nextYear, nextMonth1Based, dayNum)

                val isToday = (nextYear == todayYear && nextMonth1Based == todayMonth && dayNum == todayDayNum)
                val isSelected = false

                val dayObj = CalendarDay(
                    dayOfMonth = dayNum,
                    gregorianDateKey = gDateKey,
                    fullGregorianFormatted = fullGDate,
                    hijriDay = hDayStr,
                    hijriMonthNumber = hMonthNum,
                    hijriMonthName = hMonthName,
                    hijriYear = hYearStr,
                    fullHijriFormatted = fullHDate,
                    isToday = isToday,
                    isSelected = isSelected,
                    isInCurrentMonth = false,
                    isSunday = isColumnSunday,
                    isFriday = isColumnFriday,
                    javanesePasaran = pasaran,
                    observance = obs
                )
                daysInGrid.add(dayObj)
            } else {
                val dayNum = i - firstDayColumnIndex + 1

                val gCal = Calendar.getInstance()
                gCal.set(year, month1Based - 1, dayNum)
                val fullGDate = HijriCalendarHelper.formatFullGregorianDate(gCal, isEng)
                val gDateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month1Based, dayNum)
                val pasaran = HijriCalendarHelper.getJavanesePasaran(year, month1Based, dayNum)

                val rawData = apiDaysMap?.get(dayNum)
                val hijriDayStr: String
                val hijriMonthNum: Int
                val hijriMonthName: String
                val hijriYearStr: String
                val obs: String?

                if (rawData != null) {
                    hijriDayStr = rawData.hijriDay
                    hijriMonthNum = rawData.hijriMonthNumber
                    hijriMonthName = rawData.hijriMonthName
                    hijriYearStr = rawData.hijriYear
                    obs = rawData.observance ?: HijriCalendarHelper.getGregorianHoliday(year, month1Based, dayNum)
                } else {
                    val calc = HijriCalendarHelper.convertGregorianToHijri(year, month1Based, dayNum)
                    hijriDayStr = calc.day.toString()
                    hijriMonthNum = calc.monthNumber
                    hijriMonthName = calc.monthName
                    hijriYearStr = calc.year
                    obs = HijriCalendarHelper.getVerifiedObservance(calc.day, calc.monthNumber, calc.year)
                        ?: HijriCalendarHelper.getGregorianHoliday(year, month1Based, dayNum)
                }

                val fullHDate = "$hijriDayStr $hijriMonthName $hijriYearStr H"
                val isToday = (year == todayYear && month1Based == todayMonth && dayNum == todayDayNum)
                val isSelected = (selectedDayNumber != null && dayNum == selectedDayNumber)

                val dayObj = CalendarDay(
                    dayOfMonth = dayNum,
                    gregorianDateKey = gDateKey,
                    fullGregorianFormatted = fullGDate,
                    hijriDay = hijriDayStr,
                    hijriMonthNumber = hijriMonthNum,
                    hijriMonthName = hijriMonthName,
                    hijriYear = hijriYearStr,
                    fullHijriFormatted = fullHDate,
                    isToday = isToday,
                    isSelected = isSelected,
                    isInCurrentMonth = true,
                    isSunday = isColumnSunday,
                    isFriday = isColumnFriday,
                    javanesePasaran = pasaran,
                    observance = obs
                )

                if (isSelected) {
                    activeSelectedDay = dayObj
                }

                daysInGrid.add(dayObj)
            }
        }

        val gregorianMonthTitle = "${HijriCalendarHelper.getGregorianMonthName(month1Based - 1, isEng)} $year"
        val hijriMonthSubtitle = extractHijriMonthSubtitle(daysInGrid)

        CalendarMonthData(
            year = year,
            month = month1Based,
            gregorianMonthTitle = gregorianMonthTitle,
            hijriMonthSubtitle = hijriMonthSubtitle,
            days = daysInGrid,
            selectedDay = activeSelectedDay,
            isCached = isCached,
            isOnline = isOnline,
            statusText = statusText
        )
    }

    private fun extractHijriMonthSummary(map: Map<Int, RawHijriData>): String {
        val months = map.values.map { "${it.hijriMonthName} ${it.hijriYear}" }.distinct()
        return months.joinToString(" – ")
    }

    private fun extractHijriMonthSubtitle(days: List<CalendarDay>): String {
        val validDays = days.filter { it.isInCurrentMonth }
        if (validDays.isEmpty()) return ""

        val first = validDays.first()
        val last = validDays.last()

        return if (first.hijriMonthName == last.hijriMonthName && first.hijriYear == last.hijriYear) {
            "${first.hijriMonthName} ${first.hijriYear} H"
        } else if (first.hijriYear == last.hijriYear) {
            "${first.hijriMonthName} – ${last.hijriMonthName} ${first.hijriYear} H"
        } else {
            "${first.hijriMonthName} ${first.hijriYear} H – ${last.hijriMonthName} ${last.hijriYear} H"
        }
    }

    private data class RawHijriData(
        val hijriDay: String,
        val hijriMonthNumber: Int,
        val hijriMonthName: String,
        val hijriYear: String,
        val observance: String?
    )

    companion object {
        @Volatile
        private var INSTANCE: CalendarRepository? = null

        fun getInstance(context: Context): CalendarRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CalendarRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
