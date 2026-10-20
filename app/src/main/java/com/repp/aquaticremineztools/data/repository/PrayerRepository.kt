package com.repp.aquaticremineztools.data.repository

import android.content.Context
import com.repp.aquaticremineztools.core.settings.PrayerPreferences
import com.repp.aquaticremineztools.data.local.prayer.PrayerDatabase
import com.repp.aquaticremineztools.data.local.prayer.PrayerEntity
import com.repp.aquaticremineztools.data.model.PrayerSchedule
import com.repp.aquaticremineztools.data.remote.prayer.AladhanApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrayerRepository(context: Context) {

    private val prayerDao = PrayerDatabase.getInstance(context).prayerDao()

    suspend fun getPrayerSchedule(
        context: Context,
        dateKey: String,
        latitude: Double,
        longitude: Double,
        cityName: String,
        method: Int = 20
    ): Result<PrayerSchedule> = withContext(Dispatchers.IO) {
        val safeCity = cityName.ifEmpty { PrayerPreferences.getLastCity(context) }

        // 1. Check local database cache for this date & city
        val cachedEntity = prayerDao.getSchedule(dateKey, safeCity)
            ?: prayerDao.getScheduleByDate(dateKey)
            ?: prayerDao.getLatestSchedule()

        try {
            // 2. Fetch from Aladhan API
            val response = AladhanApiClient.service.getTimings(
                date = dateKey,
                latitude = latitude,
                longitude = longitude,
                method = method
            )

            if (response.isSuccessful && response.body()?.code == 200) {
                val timings = response.body()?.data?.timings
                if (timings != null) {
                    val imsak = cleanTime(timings.imsak) ?: "04:41"
                    val subuh = cleanTime(timings.fajr) ?: "04:51"
                    val dzuhur = cleanTime(timings.dhuhr) ?: "11:56"
                    val ashar = cleanTime(timings.asr) ?: "15:18"
                    val maghrib = cleanTime(timings.maghrib) ?: "17:55"
                    val isya = cleanTime(timings.isha) ?: "19:05"

                    val entity = PrayerEntity(
                        dateKey = dateKey,
                        cityKey = safeCity,
                        latitude = latitude,
                        longitude = longitude,
                        imsak = imsak,
                        subuh = subuh,
                        dzuhur = dzuhur,
                        ashar = ashar,
                        maghrib = maghrib,
                        isya = isya,
                        updatedAt = System.currentTimeMillis()
                    )

                    // Insert into DB & update preferences
                    prayerDao.insertSchedule(entity)
                    PrayerPreferences.saveLastLocation(context, latitude, longitude, safeCity)

                    val schedule = PrayerSchedule(
                        dateKey = dateKey,
                        cityName = safeCity,
                        latitude = latitude,
                        longitude = longitude,
                        imsak = imsak,
                        subuh = subuh,
                        dzuhur = dzuhur,
                        ashar = ashar,
                        maghrib = maghrib,
                        isya = isya,
                        isCached = false,
                        updatedAt = entity.updatedAt
                    )
                    return@withContext Result.success(schedule)
                }
            }
        } catch (e: Exception) {
            // Network or parsing error
        }

        // 3. Fallback to cached data if network failed
        if (cachedEntity != null) {
            val cachedSchedule = PrayerSchedule(
                dateKey = cachedEntity.dateKey,
                cityName = cachedEntity.cityKey,
                latitude = cachedEntity.latitude,
                longitude = cachedEntity.longitude,
                imsak = cachedEntity.imsak,
                subuh = cachedEntity.subuh,
                dzuhur = cachedEntity.dzuhur,
                ashar = cachedEntity.ashar,
                maghrib = cachedEntity.maghrib,
                isya = cachedEntity.isya,
                isCached = true,
                updatedAt = cachedEntity.updatedAt
            )
            return@withContext Result.success(cachedSchedule)
        }

        return@withContext Result.failure(Exception("No network and no cached prayer schedule available"))
    }

    private fun cleanTime(raw: String?): String? {
        if (raw == null) return null
        val timePart = raw.trim().split(" ")[0]
        return if (timePart.matches(Regex("""\d{1,2}:\d{2}"""))) timePart else null
    }

    companion object {
        @Volatile
        private var INSTANCE: PrayerRepository? = null

        fun getInstance(context: Context): PrayerRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PrayerRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
