package com.repp.aquaticremineztools.core.settings

import android.content.Context
import android.content.SharedPreferences

object NewsPreferences {
    private const val PREF_NAME = "aquatic_news_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    val DEFAULT_CATEGORIES = listOf(
        "Technology",
        "Sports",
        "Business",
        "Lifestyle",
        "Health",
        "Automotive",
        "Entertainment"
    )

    val DEFAULT_FAVORITES = setOf("Technology", "Sports", "Business")

    fun getFavoriteCategories(context: Context): Set<String> {
        return getPrefs(context).getStringSet("favorite_categories", DEFAULT_FAVORITES) ?: DEFAULT_FAVORITES
    }

    fun setFavoriteCategories(context: Context, favorites: Set<String>) {
        getPrefs(context).edit().putStringSet("favorite_categories", favorites).apply()
    }

    fun getNewsLanguage(context: Context): String {
        return getPrefs(context).getString("news_language", "all") ?: "all"
    }

    fun setNewsLanguage(context: Context, language: String) {
        getPrefs(context).edit().putString("news_language", language).apply()
    }

    fun getNewsTheme(context: Context): String {
        return getPrefs(context).getString("news_theme", "dark") ?: "dark"
    }

    fun setNewsTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString("news_theme", theme).apply()
    }

    fun getLastFetchTimestamp(context: Context): Long {
        return getPrefs(context).getLong("last_fetch_timestamp", 0L)
    }

    fun setLastFetchTimestamp(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong("last_fetch_timestamp", timestamp).apply()
    }
}
