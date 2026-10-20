package com.repp.aquaticremineztools.core.util

import com.repp.aquaticremineztools.data.model.PrayerSchedule
import java.util.Calendar
import java.util.Locale

data class NextPrayerInfo(
    val currentPrayerName: String,
    val prayerName: String,
    val targetTimeMillis: Long,
    val diffMillis: Long,
    val countdownText: String
)

object PrayerCalculator {

    fun calculateNextPrayer(
        schedule: PrayerSchedule,
        nowCalendar: Calendar = Calendar.getInstance()
    ): NextPrayerInfo {
        val nowMillis = nowCalendar.timeInMillis

        // Parse obligatory prayer times for today
        val subuhToday = parseTimeToCalendar(schedule.subuh, nowCalendar, 0)
        val dzuhurToday = parseTimeToCalendar(schedule.dzuhur, nowCalendar, 0)
        val asharToday = parseTimeToCalendar(schedule.ashar, nowCalendar, 0)
        val maghribToday = parseTimeToCalendar(schedule.maghrib, nowCalendar, 0)
        val isyaToday = parseTimeToCalendar(schedule.isya, nowCalendar, 0)

        val targetCalendar: Calendar
        val nextName: String
        val currentName: String

        when {
            nowMillis < subuhToday.timeInMillis -> {
                currentName = "Isya"
                nextName = "Subuh"
                targetCalendar = subuhToday
            }
            nowMillis < dzuhurToday.timeInMillis -> {
                currentName = "Subuh"
                nextName = "Dzuhur"
                targetCalendar = dzuhurToday
            }
            nowMillis < asharToday.timeInMillis -> {
                currentName = "Dzuhur"
                nextName = "Ashar"
                targetCalendar = asharToday
            }
            nowMillis < maghribToday.timeInMillis -> {
                currentName = "Ashar"
                nextName = "Maghrib"
                targetCalendar = maghribToday
            }
            nowMillis < isyaToday.timeInMillis -> {
                currentName = "Maghrib"
                nextName = "Isya"
                targetCalendar = isyaToday
            }
            else -> {
                // After Isya today -> Current is Isya, Next obligatory prayer is Subuh tomorrow
                currentName = "Isya"
                nextName = "Subuh"
                targetCalendar = parseTimeToCalendar(schedule.subuh, nowCalendar, 1)
            }
        }

        val diffMillis = (targetCalendar.timeInMillis - nowMillis).coerceAtLeast(0L)
        val countdownText = formatCountdown(diffMillis)

        return NextPrayerInfo(
            currentPrayerName = currentName,
            prayerName = nextName,
            targetTimeMillis = targetCalendar.timeInMillis,
            diffMillis = diffMillis,
            countdownText = countdownText
        )
    }

    private fun parseTimeToCalendar(
        timeStr: String,
        baseCalendar: Calendar,
        addDays: Int
    ): Calendar {
        val cal = baseCalendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, addDays)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val parts = timeStr.trim().split(" ")[0].split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)

        return cal
    }

    fun formatCountdown(diffMillis: Long): String {
        val totalSeconds = diffMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
