package com.repp.aquaticremineztools.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PinManager {

    private const val PREFS_NAME = "secure_pin_prefs"
    private const val KEY_PIN = "pin"
    private const val KEY_QUESTION = "security_question"
    private const val KEY_ANSWER = "security_answer"
    private const val KEY_LOCK_ENABLED = "lock_enabled"
    private const val KEY_LAST_UNLOCK = "last_unlock"

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
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun hasPin(context: Context): Boolean {
        return getPrefs(context).getString(KEY_PIN, null) != null
    }

    fun savePin(
        context: Context,
        pin: String,
        question: String,
        answer: String,
    ) {
        getPrefs(context).edit()
            .putString(KEY_PIN, pin)
            .putString(KEY_QUESTION, question)
            .putString(KEY_ANSWER, answer.lowercase().trim())
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val savedPin = getPrefs(context).getString(KEY_PIN, null) ?: return false
        return savedPin == pin
    }

    fun verifyAnswer(context: Context, answer: String): Boolean {
        val savedAnswer = getPrefs(context).getString(KEY_ANSWER, null) ?: return false
        return savedAnswer == answer.lowercase().trim()
    }

    fun getSecurityQuestion(context: Context): String {
        return getPrefs(context).getString(KEY_QUESTION, "Siapa namamu?")
            ?: "Siapa namamu?"
    }

    fun updatePin(context: Context, newPin: String) {
        getPrefs(context).edit()
            .putString(KEY_PIN, newPin)
            .apply()
    }

    fun deletePin(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PIN)
            .remove(KEY_QUESTION)
            .remove(KEY_ANSWER)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .apply()
    }

    fun isLockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCK_ENABLED, false)
    }

    fun setLastUnlock(context: Context) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_UNLOCK, System.currentTimeMillis())
            .apply()
    }

    fun needsVerification(context: Context): Boolean {
        if (!hasPin(context)) return false
        if (!isLockEnabled(context)) return false
        val lastUnlock = getPrefs(context).getLong(KEY_LAST_UNLOCK, 0)
        return System.currentTimeMillis() - lastUnlock > 5_000
    }

    fun resetAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}