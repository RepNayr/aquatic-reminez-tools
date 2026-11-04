package com.repp.aquaticremineztools.domain.compressor

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FolderImportHelper {

    fun scanFolderForImages(context: Context, treeUri: Uri): List<Uri> {
        val imageUris = mutableListOf<Uri>()
        try {
            val parsedDoc = DocumentFile.fromTreeUri(context, treeUri)
            parsedDoc?.listFiles()?.forEach { file ->
                if (file.isFile && isImageFile(file.name ?: "")) {
                    imageUris.add(file.uri)
                } else if (file.isDirectory) {
                    scanSubfolder(context, file, imageUris)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imageUris
    }

    private fun scanSubfolder(context: Context, folder: DocumentFile, outList: MutableList<Uri>) {
        try {
            folder.listFiles().forEach { file ->
                if (file.isFile && isImageFile(file.name ?: "")) {
                    outList.add(file.uri)
                } else if (file.isDirectory) {
                    scanSubfolder(context, file, outList)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.endsWith(".heic") || lower.endsWith(".heif") ||
                lower.endsWith(".avif") || lower.endsWith(".bmp")
    }
}
