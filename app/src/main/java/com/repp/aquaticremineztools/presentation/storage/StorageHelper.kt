package com.repp.aquaticremineztools.presentation.storage

import android.content.Context
import java.io.File

object StorageHelper {

    private const val FOLDER_NAME = "AquaticStorage"

    fun getStorageDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listFiles(context: Context): List<StorageFile> {
        val dir = getStorageDir(context)
        val files = dir.listFiles() ?: return emptyList()

        return files
            .filter { it.isFile }
            .map { file ->
                StorageFile(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    type = detectType(file.name),
                )
            }
            .sortedByDescending { it.lastModified }
    }

    fun detectType(fileName: String): StorageFile.FileType {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif" ->
                StorageFile.FileType.IMAGE
            "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv" ->
                StorageFile.FileType.VIDEO
            "mp3", "wav", "aac", "ogg", "flac", "m4a", "opus" ->
                StorageFile.FileType.AUDIO
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx",
            "csv", "json", "xml", "html", "md" ->
                StorageFile.FileType.DOCUMENT
            else -> StorageFile.FileType.OTHER
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("in", "ID"))
        return sdf.format(java.util.Date(timestamp))
    }

    fun deleteFile(file: File): Boolean = file.delete()

    fun renameFile(file: File, newName: String): File? {
        val newFile = File(file.parent, newName)
        return if (file.renameTo(newFile)) newFile else null
    }
}