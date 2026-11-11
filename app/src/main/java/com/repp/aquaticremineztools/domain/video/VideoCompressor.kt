package com.repp.aquaticremineztools.domain.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            retriever.setDataSource(sourceFile.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        val durationSec = (durationMs / 1000L).coerceAtLeast(1L)
        val audioBitrate = 64_000L
        val targetBitrate = (((targetSizeBytes * 8L) / durationSec) - audioBitrate).coerceIn(200_005L, 5_000_000L)

        onProgress(5)

        val mediaItem = MediaItem.fromUri(Uri.fromFile(sourceFile))

        suspendCancellableCoroutine { continuation ->
            var transformer: Transformer? = null
            try {
                transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (outputFile.exists() && outputFile.length() > 0L) {
                                onProgress(100)
                                if (continuation.isActive) continuation.resume(Unit)
                            } else {
                                if (continuation.isActive) continuation.resumeWithException(IOException("File hasil transcode kosong."))
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException("Transcoding gagal: ${exportException.message}", exportException))
                            }
                        }
                    })
                    .build()

                transformer.start(mediaItem, outputFile.absolutePath)

                continuation.invokeOnCancellation {
                    try {
                        transformer?.cancel()
                    } catch (_: Exception) {}
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                }
            } catch (e: Exception) {
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                if (continuation.isActive) {
                    continuation.resumeWithException(IOException("Inisialisasi Transformer gagal: ${e.message}", e))
                }
            }
        }

        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw IOException("File video terkompresi tidak ditemukan.")
        }

        val validatorRetriever = MediaMetadataRetriever()
        try {
            validatorRetriever.setDataSource(outputFile.absolutePath)
            val outDuration = validatorRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (outDuration <= 0L) {
                throw IOException("File terkompresi tidak memiliki durasi valid.")
            }
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            throw IOException("Validasi file terkompresi gagal: ${e.message}", e)
        } finally {
            try { validatorRetriever.release() } catch (_: Exception) {}
        }

        return@withContext outputFile
    }
}
