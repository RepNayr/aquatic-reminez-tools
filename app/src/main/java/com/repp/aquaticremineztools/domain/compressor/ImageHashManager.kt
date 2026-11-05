package com.repp.aquaticremineztools.domain.compressor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object ImageHashManager {

    private val mutex = Mutex()
    private val activeSessionHashes = mutableMapOf<String, String>() // itemId -> sha256

    suspend fun computeSha256(file: File): String? = withContext(Dispatchers.IO) {
        var result = calculateHash(file)
        if (result == null) {
            // Single retry on hash calculation failure
            result = calculateHash(file)
        }
        result
    }

    private fun calculateHash(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun registerHashAndCheckDuplicate(itemId: String, sha256: String): String? = mutex.withLock {
        val existingEntry = activeSessionHashes.entries.firstOrNull { it.value == sha256 }
        if (existingEntry != null) {
            return@withLock existingEntry.key
        }
        activeSessionHashes[itemId] = sha256
        return@withLock null
    }

    suspend fun unregisterHash(itemId: String) = mutex.withLock {
        activeSessionHashes.remove(itemId)
    }

    suspend fun clearSessionHashes() = mutex.withLock {
        activeSessionHashes.clear()
    }
}
