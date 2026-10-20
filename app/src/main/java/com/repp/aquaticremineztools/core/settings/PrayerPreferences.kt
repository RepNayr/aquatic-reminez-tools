package com.repp.aquaticremineztools.core.settings

import android.content.Context
import android.content.SharedPreferences

object PrayerPreferences {
    private const val PREF_NAME = "aquatic_prayer_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private const val KEY_LAT = "last_lat"
    private const val KEY_LON = "last_lon"
    private const val KEY_CITY = "last_city"
    private const val KEY_METHOD = "calc_method"

    // Default: Jakarta (-6.2088, 106.8456)
    fun getLastLatitude(context: Context): Double {
        val lat = getPrefs(context).getString(KEY_LAT, "-6.2088") ?: "-6.2088"
        return lat.toDoubleOrNull() ?: -6.2088
    }

    fun getLastLongitude(context: Context): Double {
        val lon = getPrefs(context).getString(KEY_LON, "106.8456") ?: "106.8456"
        return lon.toDoubleOrNull() ?: 106.8456
    }

    fun getLastCity(context: Context): String {
        return getPrefs(context).getString(KEY_CITY, "Jakarta") ?: "Jakarta"
    }

    fun saveLastLocation(context: Context, lat: Double, lon: Double, city: String) {
        getPrefs(context).edit()
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LON, lon.toString())
            .putString(KEY_CITY, city.ifEmpty { "Jakarta" })
            .apply()
    }

    fun getCalculationMethod(context: Context): Int {
        return getPrefs(context).getInt(KEY_METHOD, 20) // 20 = KEMENAG Indonesia
    }

    fun setCalculationMethod(context: Context, method: Int) {
        getPrefs(context).edit().putInt(KEY_METHOD, method).apply()
    }
}
