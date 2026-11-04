package com.repp.aquaticremineztools.domain.compressor

import android.content.Context
import android.os.Environment
import java.util.Locale

object StorageManager {

    data class StorageCheckResult(
        val isSufficient: Boolean,
        val usableBytes: Long,
        val requiredBytes: Long,
        val warningMessage: String? = null
    )

    /**
     * Checks if available storage on cache/internal/external storage has enough space
     * for the estimated batch output (applying a 1.5x safety multiplier).
     */
    fun checkAvailableStorage(context: Context, estimatedBatchOutputBytes: Long): StorageCheckResult {
        return try {
            val cacheDir = context.cacheDir
            val usableCache = cacheDir.usableSpace

            val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: cacheDir
            val usableExternal = externalDir.usableSpace

            val minUsable = minOf(usableCache, usableExternal)
            val safetyMultiplier = 1.5
            val requiredWithBuffer = (estimatedBatchOutputBytes * safetyMultiplier).toLong().coerceAtLeast(10 * 1024 * 1024L)

            val isSufficient = minUsable >= requiredWithBuffer

            val usableMb = minUsable / (1024.0 * 1024.0)
            val requiredMb = requiredWithBuffer / (1024.0 * 1024.0)

            val warning = if (!isSufficient) {
                String.format(
                    Locale.US,
                    "Insufficient storage space: %.1f MB available, but at least %.1f MB is required.",
                    usableMb,
                    requiredMb
                )
            } else null

            StorageCheckResult(
                isSufficient = isSufficient,
                usableBytes = minUsable,
                requiredBytes = requiredWithBuffer,
                warningMessage = warning
            )
        } catch (e: Exception) {
            e.printStackTrace()
            StorageCheckResult(
                isSufficient = true,
                usableBytes = Long.MAX_VALUE,
                requiredBytes = estimatedBatchOutputBytes
            )
        }
    }
}
