package com.repp.aquaticremineztools.core.security

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object CrotPediaSessionStore {

    private const val PREFS_NAME = "crotpedia_secure_session_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_LAST_LOGIN_TIME = "last_login_time"
    private const val KEY_COOKIES_MAP = "cookies_map"

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getLastLoginTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_LOGIN_TIME, 0L)
    }

    fun saveSession(context: Context, cookies: String) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            .putString(KEY_COOKIES_MAP, cookies)
            .apply()
    }

    fun getSavedCookies(context: Context): String? {
        return getPrefs(context).getString(KEY_COOKIES_MAP, null)
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.removeSessionCookies(null)
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        } catch (_: Exception) {}
    }

    fun isAllowedCookieHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val lowerHost = host.lowercase()
        return lowerHost == "crotpedia.net" || lowerHost.endsWith(".crotpedia.net")
    }
}
