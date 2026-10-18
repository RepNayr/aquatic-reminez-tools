package com.repp.aquaticremineztools.core.settings

import android.content.Context
import android.content.SharedPreferences

object SettingsPreferences {
    private const val PREF_NAME = "aquatic_settings_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // --- Stage 01: Appearance & Personalization ---
    fun getThemeMode(context: Context): String = getPrefs(context).getString("theme_mode", "system") ?: "system"
    fun setThemeMode(context: Context, mode: String) = getPrefs(context).edit().putString("theme_mode", mode).apply()

    fun getAccentColor(context: Context): String = getPrefs(context).getString("accent_color", "cyan") ?: "cyan"
    fun setAccentColor(context: Context, color: String) = getPrefs(context).edit().putString("accent_color", color).apply()

    fun getWallpaperUri(context: Context): String = getPrefs(context).getString("wallpaper_uri", "") ?: ""
    fun setWallpaperUri(context: Context, uri: String) = getPrefs(context).edit().putString("wallpaper_uri", uri).apply()

    fun getWallpaperOpacity(context: Context): Float = getPrefs(context).getFloat("wallpaper_opacity", 0.8f)
    fun setWallpaperOpacity(context: Context, opacity: Float) = getPrefs(context).edit().putFloat("wallpaper_opacity", opacity).apply()

    fun getGlassTransparency(context: Context): Float = getPrefs(context).getFloat("glass_transparency", 0.75f)
    fun setGlassTransparency(context: Context, trans: Float) = getPrefs(context).edit().putFloat("glass_transparency", trans).apply()

    fun getBlurIntensity(context: Context): Int = getPrefs(context).getInt("blur_intensity", 15)
    fun setBlurIntensity(context: Context, intensity: Int) = getPrefs(context).edit().putInt("blur_intensity", intensity).apply()

    fun getFontSize(context: Context): String = getPrefs(context).getString("font_size", "default") ?: "default"
    fun setFontSize(context: Context, size: String) = getPrefs(context).edit().putString("font_size", size).apply()

    fun getUiDensity(context: Context): String = getPrefs(context).getString("ui_density", "comfortable") ?: "comfortable"
    fun setUiDensity(context: Context, density: String) = getPrefs(context).edit().putString("ui_density", density).apply()

    fun getCardStyle(context: Context): String = getPrefs(context).getString("card_style", "glass") ?: "glass"
    fun setCardStyle(context: Context, style: String) = getPrefs(context).edit().putString("card_style", style).apply()

    fun isReduceMotion(context: Context): Boolean = getPrefs(context).getBoolean("reduce_motion", false)
    fun setReduceMotion(context: Context, reduce: Boolean) = getPrefs(context).edit().putBoolean("reduce_motion", reduce).apply()

    fun getLanguage(context: Context): String = getPrefs(context).getString("language", "en") ?: "en"
    fun setLanguage(context: Context, lang: String) = getPrefs(context).edit().putString("language", lang).apply()

    fun isHighContrast(context: Context): Boolean = getPrefs(context).getBoolean("high_contrast", false)
    fun setHighContrast(context: Context, hc: Boolean) = getPrefs(context).edit().putBoolean("high_contrast", hc).apply()

    // --- Stage 02: Security & Data ---
    fun isAppLockEnabled(context: Context): Boolean = getPrefs(context).getBoolean("app_lock_enabled", false)
    fun setAppLockEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("app_lock_enabled", enabled).apply()

    fun getAutoLockTimeout(context: Context): String = getPrefs(context).getString("auto_lock_timeout", "1_min") ?: "1_min"
    fun setAutoLockTimeout(context: Context, timeout: String) = getPrefs(context).edit().putString("auto_lock_timeout", timeout).apply()

    fun isBiometricEnabled(context: Context): Boolean = getPrefs(context).getBoolean("biometric_enabled", true)
    fun setBiometricEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("biometric_enabled", enabled).apply()

    fun isPrivacyShieldEnabled(context: Context): Boolean = getPrefs(context).getBoolean("privacy_shield", true)
    fun setPrivacyShieldEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("privacy_shield", enabled).apply()

    fun isScreenshotProtectionEnabled(context: Context): Boolean = getPrefs(context).getBoolean("screenshot_protection", false)
    fun setScreenshotProtectionEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("screenshot_protection", enabled).apply()

    fun isHistoryLoggingEnabled(context: Context): Boolean = getPrefs(context).getBoolean("history_logging", true)
    fun setHistoryLogging(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("history_logging", enabled).apply()

    fun getHistoryRetentionDays(context: Context): Int = getPrefs(context).getInt("history_retention_days", 30)
    fun setHistoryRetentionDays(context: Context, days: Int) = getPrefs(context).edit().putInt("history_retention_days", days).apply()

    fun isExecutionConfirmationEnabled(context: Context): Boolean = getPrefs(context).getBoolean("execution_confirmation", true)
    fun setExecutionConfirmation(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("execution_confirmation", enabled).apply()

    // --- Stage 03: App Info, Games, Support ---
    fun getGameSoundMode(context: Context): Boolean = getPrefs(context).getBoolean("game_sound", true)
    fun setGameSoundMode(context: Context, sound: Boolean) = getPrefs(context).edit().putBoolean("game_sound", sound).apply()

    fun getGameVibrationMode(context: Context): Boolean = getPrefs(context).getBoolean("game_vibration", true)
    fun setGameVibrationMode(context: Context, vib: Boolean) = getPrefs(context).edit().putBoolean("game_vibration", vib).apply()

    // --- Stage 04: Tool Workspace & Workflows ---
    fun getFavorites(context: Context): Set<String> = getPrefs(context).getStringSet("favorites_set", emptySet()) ?: emptySet()
    fun setFavorites(context: Context, favorites: Set<String>) = getPrefs(context).edit().putStringSet("favorites_set", favorites).apply()

    fun getPinnedQuickMenu(context: Context): Set<String> = getPrefs(context).getStringSet("pinned_quick_menu", setOf("net_info", "ping", "dns_lookup", "port_scan")) ?: setOf("net_info", "ping", "dns_lookup", "port_scan")
    fun setPinnedQuickMenu(context: Context, pinned: Set<String>) = getPrefs(context).edit().putStringSet("pinned_quick_menu", pinned).apply()

    fun getWorkspaceProfile(context: Context): String = getPrefs(context).getString("workspace_profile", "Default") ?: "Default"
    fun setWorkspaceProfile(context: Context, profile: String) = getPrefs(context).edit().putString("workspace_profile", profile).apply()
}
