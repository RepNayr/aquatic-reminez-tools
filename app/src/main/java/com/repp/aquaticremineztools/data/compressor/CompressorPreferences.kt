package com.repp.aquaticremineztools.data.compressor

import android.content.Context
import android.content.SharedPreferences

object CompressorPreferences {
    private const val PREF_NAME = "aquatic_image_compressor_prefs"

    private const val KEY_CLOUD_CONSENT = "cloud_consent"
    private const val KEY_NETWORK_PREF = "network_preference"
    private const val KEY_DEFAULT_MODE = "default_mode"
    private const val KEY_DEFAULT_QUALITY = "default_quality"
    private const val KEY_DEFAULT_TARGET_SIZE_KB = "default_target_size_kb"
    private const val KEY_DEFAULT_PRESET = "default_preset"
    private const val KEY_DEFAULT_FORMAT = "default_format"
    private const val KEY_STRIP_GPS = "strip_gps"
    private const val KEY_PRESERVE_ICC = "preserve_icc"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getCloudConsent(context: Context): CloudConsentState {
        val raw = getPrefs(context).getString(KEY_CLOUD_CONSENT, CloudConsentState.NOT_SET.name)
        return try {
            CloudConsentState.valueOf(raw ?: CloudConsentState.NOT_SET.name)
        } catch (e: Exception) {
            CloudConsentState.NOT_SET
        }
    }

    fun setCloudConsent(context: Context, state: CloudConsentState) {
        getPrefs(context).edit().putString(KEY_CLOUD_CONSENT, state.name).apply()
    }

    fun getNetworkPreference(context: Context): NetworkPreference {
        val raw = getPrefs(context).getString(KEY_NETWORK_PREF, NetworkPreference.WIFI_ONLY.name)
        return try {
            NetworkPreference.valueOf(raw ?: NetworkPreference.WIFI_ONLY.name)
        } catch (e: Exception) {
            NetworkPreference.WIFI_ONLY
        }
    }

    fun setNetworkPreference(context: Context, pref: NetworkPreference) {
        getPrefs(context).edit().putString(KEY_NETWORK_PREF, pref.name).apply()
    }

    fun getStripGps(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STRIP_GPS, true)
    }

    fun setStripGps(context: Context, strip: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STRIP_GPS, strip).apply()
    }

    fun getPreserveIcc(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PRESERVE_ICC, true)
    }

    fun setPreserveIcc(context: Context, preserve: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PRESERVE_ICC, preserve).apply()
    }
}
