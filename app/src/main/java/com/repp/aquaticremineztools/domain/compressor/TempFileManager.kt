package com.repp.aquaticremineztools.domain.compressor

import android.content.Context
import java.io.File

object TempFileManager {

    /**
     * Cleans up orphaned temporary files in cache directory older than maxAgeMs (default 24 hours).
     * Never deletes active item paths or source input files.
     */
    fun cleanOrphanedTempFiles(
        context: Context,
        activeItemPaths: Set<String>,
        maxAgeMs: Long = 24 * 60 * 60 * 1000L
    ) {
        try {
            val tempDir = ImageImportManager.getTempDir(context)
            val files = tempDir.listFiles() ?: return
            val now = System.currentTimeMillis()

            for (file in files) {
                if (!file.isFile) continue

                val path = file.absolutePath
                if (activeItemPaths.contains(path)) continue

                val age = now - file.lastModified()
                if (age > maxAgeMs) {
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteFileSafely(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        return try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
