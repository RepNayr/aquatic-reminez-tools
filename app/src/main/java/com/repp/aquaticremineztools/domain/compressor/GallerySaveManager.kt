package com.repp.aquaticremineztools.domain.compressor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
        val errorMessage: String? = null
    )

    fun saveToGallery(context: Context, sourceFile: File, originalFileName: String, mimeType: String): SaveResult {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            return SaveResult(false, errorMessage = "Source file does not exist or is empty.")
        }

        val baseName = getBaseFileName(originalFileName)
        val ext = getExtension(originalFileName, mimeType)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val formattedFileName = "${baseName}_$timestamp.$ext"

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
                return SaveResult(false, errorMessage = "Failed to create MediaStore entry.")
            }

            contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            } ?: return SaveResult(false, errorMessage = "Failed to open output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }

            val savedPath = "$albumName/$formattedFileName"

            // Delete temporary app-side result copy after confirmed commit
            try {
                if (sourceFile.exists()) {
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
            SaveResult(false, errorMessage = e.localizedMessage ?: "Storage write failure.")
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
