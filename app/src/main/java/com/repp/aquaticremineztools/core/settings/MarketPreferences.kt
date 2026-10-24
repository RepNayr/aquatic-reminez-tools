package com.repp.aquaticremineztools.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.repp.aquaticremineztools.data.model.MarketCategory

class MarketPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("market_preferences", Context.MODE_PRIVATE)

    var isDemoModeEnabled: Boolean
        get() = prefs.getBoolean("key_demo_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("key_demo_mode_enabled", value).apply()

    var defaultCategory: MarketCategory
        get() {
            val savedName = prefs.getString("key_default_category", MarketCategory.OVERVIEW.name)
            return try {
                MarketCategory.valueOf(savedName ?: MarketCategory.OVERVIEW.name)
            } catch (_: Exception) {
                MarketCategory.OVERVIEW
            }
        }
        set(value) = prefs.edit().putString("key_default_category", value.name).apply()

    var baseCurrency: String
        get() = prefs.getString("key_base_currency", "USD") ?: "USD"
        set(value) = prefs.edit().putString("key_base_currency", value).apply()

    companion object {
        @Volatile
        private var INSTANCE: MarketPreferences? = null

        fun getInstance(context: Context): MarketPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = MarketPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
