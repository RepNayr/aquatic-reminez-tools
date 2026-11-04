package com.repp.aquaticremineztools.domain.compressor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GallerySaveManager {

    data class SaveResult(
        val isSuccess: Boolean,
        val savedUri: Uri? = null,
        val displayPath: String? = null,
        val errorCategory: String? = null,
        val errorMessage: String? = null
    )

    suspend fun saveToGalleryWithRetry(
        context: Context,
        sourceFile: File,
        originalFileName: String,
        mimeType: String,
        alreadySavedPath: String? = null,
        maxRetries: Int = 3
    ): SaveResult {
        // Idempotency check: if already saved and confirmed, don't duplicate MediaStore entry
        if (!alreadySavedPath.isNullOrEmpty()) {
            return SaveResult(
                isSuccess = true,
                displayPath = alreadySavedPath
            )
        }

        var attempt = 0
        var lastResult: SaveResult? = null

        while (attempt < maxRetries) {
            attempt++
            lastResult = saveToGallery(context, sourceFile, originalFileName, mimeType)
            if (lastResult.isSuccess) {
                return lastResult
            }
            if (attempt < maxRetries) {
                val backoffMs = (500L * (1 shl (attempt - 1)))
                delay(backoffMs)
            }
        }

        return lastResult ?: SaveResult(false, errorCategory = "SAVE_FAILED", errorMessage = "Gallery save failed after $maxRetries attempts.")
    }

    fun saveToGallery(context: Context, sourceFile: File, originalFileName: String, mimeType: String): SaveResult {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            return SaveResult(false, errorCategory = "SOURCE_MISSING", errorMessage = "Source file does not exist or is empty.")
        }

        val baseName = getBaseFileName(originalFileName)
        val ext = getExtension(originalFileName, mimeType)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val formattedFileName = "${baseName}_compressed_$timestamp.$ext"

        val albumName = "Pictures/Aquatic Reminez/Compressed"
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, formattedFileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, albumName)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        var uri: Uri? = null
        return try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            uri = contentResolver.insert(collection, contentValues)
            if (uri == null) {
                return SaveResult(false, errorCategory = "MEDIASTORE_ERROR", errorMessage = "Failed to create MediaStore entry.")
            }

            contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            } ?: return SaveResult(false, errorCategory = "STORAGE_ERROR", errorMessage = "Failed to open output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }

            val savedPath = "$albumName/$formattedFileName"

            // Non-destructive cleanup of temporary working file (original file is untouched)
            try {
                if (sourceFile.exists() && sourceFile.absolutePath.contains("compressed_temp")) {
                    sourceFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            SaveResult(
                isSuccess = true,
                savedUri = uri,
                displayPath = savedPath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    contentResolver.delete(uri, null, null)
                } catch (delEx: Exception) {
                    delEx.printStackTrace()
                }
            }
            val isFull = e.message?.contains("ENOSPC") == true
            val errCat = if (isFull) "STORAGE_FULL" else "WRITE_FAILED"
            SaveResult(false, errorCategory = errCat, errorMessage = e.localizedMessage ?: "Storage write failure.")
        }
    }

    private fun getBaseFileName(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    private fun getExtension(name: String, mimeType: String): String {
        val dot = name.lastIndexOf('.')
        if (dot > 0 && dot < name.length - 1) {
            return name.substring(dot + 1).lowercase()
        }
        return when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/bmp" -> "bmp"
            else -> "jpg"
        }
    }
}
