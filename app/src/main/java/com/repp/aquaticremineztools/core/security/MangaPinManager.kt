package com.repp.aquaticremineztools.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.pow

object MangaPinManager {

    private const val PREFS_NAME = "manga_secure_pin_prefs"
    private const val KEY_PIN_HASH = "manga_pin_hash"
    private const val KEY_PIN_SALT = "manga_pin_salt"
    private const val KEY_FAILED_ATTEMPTS = "manga_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "manga_lockout_until"

    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH = 256

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
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun hasPin(context: Context): Boolean {
        return getPrefs(context).getString(KEY_PIN_HASH, null) != null
    }

    fun savePin(context: Context, pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, bytesToHex(salt))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun isLockedOut(context: Context): Boolean {
        val lockoutUntil = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getRemainingLockoutSeconds(context: Context): Long {
        val lockoutUntil = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        val diff = lockoutUntil - System.currentTimeMillis()
        return if (diff > 0) diff / 1000 else 0L
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        if (isLockedOut(context)) {
            return false
        }

        val prefs = getPrefs(context)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val salt = hexToBytes(storedSaltHex)
        val hash = hashPin(pin, salt)

        return if (storedHash == hash) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            true
        } else {
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)

            if (attempts >= 5) {
                // Progressive lockout: 5 fails -> 30s, 6 fails -> 60s, 7 fails -> 120s...
                val lockoutSeconds = (30 * 2.0.pow((attempts - 5).toDouble())).toLong().coerceAtMost(3600L)
                val lockoutUntil = System.currentTimeMillis() + (lockoutSeconds * 1000)
                editor.putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
            }

            editor.apply()
            false
        }
    }

    fun resetPinState(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    internal fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return bytesToHex(hash)
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
