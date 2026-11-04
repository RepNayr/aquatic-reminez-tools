package com.repp.aquaticremineztools.domain.compressor

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import com.repp.aquaticremineztools.data.compressor.ItemStatus
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageImportManager {

    const val MAX_FILES_PER_BATCH = 10
    const val MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB
    const val MAX_BATCH_SIZE_BYTES = 500 * 1024 * 1024L // 500 MB

    sealed class ImportResult {
        data class Success(val item: CompressorItem, val tempFile: File) : ImportResult()
        data class Error(val fileName: String, val reason: String) : ImportResult()
    }

    fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, "compressed_temp")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun clearTempDir(context: Context) {
        try {
            val dir = getTempDir(context)
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importUri(context: Context, uri: Uri): ImportResult {
        val fileName = getFileName(context, uri) ?: "image_${System.currentTimeMillis()}.jpg"
        val fileSize = getFileSize(context, uri)

        if (fileSize <= 0L) {
            return ImportResult.Error(fileName, "File is empty or inaccessible.")
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            return ImportResult.Error(fileName, "File exceeds maximum size limit of 100 MB.")
        }

        val tempDir = getTempDir(context)
        val fileId = UUID.randomUUID().toString()
        val extension = getExtension(fileName)
        val tempFile = File(tempDir, "input_${fileId}_${System.currentTimeMillis()}.$extension")

        val copySuccess = copyUriToFile(context, uri, tempFile)
        if (!copySuccess || !tempFile.exists() || tempFile.length() == 0L) {
            return ImportResult.Error(fileName, "Failed to copy image content into temporary storage.")
        }

        // Layered format validation
        val magicFormat = detectMagicFormat(tempFile)
        if (magicFormat == null) {
            tempFile.delete()
            return ImportResult.Error(fileName, "Invalid or corrupt file signature. Image format not recognized.")
        }

        // Check decoder compatibility and read dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(tempFile.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            // Some HEIC / TIFF files might not be decoded by standard BitmapFactory without ImageDecoder
            if (magicFormat != "HEIC" && magicFormat != "TIFF" && magicFormat != "AVIF") {
                tempFile.delete()
                return ImportResult.Error(fileName, "Decoder failed to parse image dimensions. File may be corrupted.")
            }
        }

        val mimeType = options.outMimeType ?: getMimeFromFormat(magicFormat)
        val width = if (options.outWidth > 0) options.outWidth else 1920
        val height = if (options.outHeight > 0) options.outHeight else 1080

        val item = CompressorItem(
            id = fileId,
            uri = uri.toString(),
            fileName = fileName,
            fileSize = tempFile.length(),
            mimeType = mimeType,
            width = width,
            height = height,
            status = ItemStatus.CHECKING,
            originalSize = tempFile.length(),
            localTempPath = tempFile.absolutePath
        )

        return ImportResult.Success(item, tempFile)
    }

    private fun detectMagicFormat(file: File): String? {
        try {
            file.inputStream().use { stream ->
                val bytes = ByteArray(12)
                val read = stream.read(bytes)
                if (read < 4) return null

                // JPEG: FF D8 FF
                if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
                    return "JPEG"
                }
                // PNG: 89 50 4E 47 0D 0A 1A 0A
                if (bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()) {
                    return "PNG"
                }
                // GIF: GIF87a / GIF89a
                if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
                    return "GIF"
                }
                // BMP: BM (42 4D)
                if (bytes[0] == 'B'.code.toByte() && bytes[1] == 'M'.code.toByte()) {
                    return "BMP"
                }
                // WebP: RIFF....WEBP
                if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
                    if (read >= 12 && bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()) {
                        return "WEBP"
                    }
                }
                // TIFF: II (49 49 2A 00) or MM (4D 4D 00 2A)
                if ((bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte()) || (bytes[0] == 0x4D.toByte() && bytes[1] == 0x4D.toByte())) {
                    return "TIFF"
                }
                // HEIC/HEIF or AVIF: ftyp box
                if (read >= 12) {
                    val boxType = String(bytes, 4, 4, Charsets.US_ASCII)
                    if (boxType == "ftyp") {
                        val brand = String(bytes, 8, 4, Charsets.US_ASCII)
                        if (brand.startsWith("hei") || brand.startsWith("mif") || brand.startsWith("heic")) {
                            return "HEIC"
                        }
                        if (brand.startsWith("avi") || brand.startsWith("avif")) {
                            return "AVIF"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getMimeFromFormat(format: String): String {
        return when (format) {
            "JPEG" -> "image/jpeg"
            "PNG" -> "image/png"
            "WEBP" -> "image/webp"
            "GIF" -> "image/gif"
            "BMP" -> "image/bmp"
            "TIFF" -> "image/tiff"
            "HEIC" -> "image/heic"
            "AVIF" -> "image/avif"
            else -> "image/jpeg"
        }
    }

    private fun getExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot >= 0 && dot < fileName.length - 1) {
            fileName.substring(dot + 1).lowercase()
        } else {
            "jpg"
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            result = cursor.getString(idx)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        var size: Long = 0L
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (idx >= 0) {
                            size = cursor.getLong(idx)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (size == 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    size = stream.available().toLong()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return size
    }

    private fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
