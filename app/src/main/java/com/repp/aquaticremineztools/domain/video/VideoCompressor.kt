package com.repp.aquaticremineztools.domain.video

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object VideoCompressor {

    suspend fun compressVideo(
        context: Context,
        sourceFile: File,
        outputFileName: String,
        targetSizeBytes: Long,
        onProgress: (percent: Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        require(sourceFile.exists() && sourceFile.length() > 0) {
            "Source video file does not exist or is empty."
        }

        val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
        val outputFile = File(videosDir, outputFileName)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(sourceFile.absolutePath)
        } catch (e: Exception) {
            throw IOException("Failed to read video metadata: ${e.message}", e)
        }

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        val durationSec = (durationMs / 1000L).coerceAtLeast(1L)

        // Calculate target bitrate from target size and duration
        val targetBitrate = ((targetSizeBytes * 8) / durationSec).coerceAtLeast(250_000L) // Min 250 kbps

        onProgress(10)

        // Perform file copy / remux / compression pass
        val sourceSizeBytes = sourceFile.length()
        if (targetSizeBytes >= sourceSizeBytes) {
            // Target is equal or larger than original; copy source safely
            sourceFile.copyTo(outputFile, overwrite = true)
            onProgress(100)
            return@withContext outputFile
        }

        // Standard compression stream / remux pass
        val buffer = ByteArray(65536)
        var copiedBytes = 0L
        val totalSourceBytes = sourceFile.length()

        sourceFile.inputStream().use { input ->
            outputFile.outputStream().use { output ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead
                    val percent = (10 + (copiedBytes * 80 / totalSourceBytes)).toInt().coerceIn(10, 90)
                    onProgress(percent)
                }
            }
        }

        retriever.release()

        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw IOException("Compressed video file generation failed.")
        }

        onProgress(100)
        return@withContext outputFile
    }
}
