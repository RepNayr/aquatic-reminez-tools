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

    suspend fun convertTsToMp4(
        context: Context,
        sourceFile: File,
        outputFileName: String,
        onProgress: (percent: Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        require(sourceFile.exists() && sourceFile.length() > 0) {
            "Source TS file does not exist or is empty."
        }

        val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
        val outputFile = File(videosDir, outputFileName)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        onProgress(10)

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
                                if (continuation.isActive) continuation.resumeWithException(IOException("File hasil konversi MP4 kosong."))
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException("Konversi TS ke MP4 gagal: ${exportException.message}", exportException))
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
            throw IOException("File video MP4 hasil konversi tidak ditemukan.")
        }

        val validatorRetriever = MediaMetadataRetriever()
        try {
            validatorRetriever.setDataSource(outputFile.absolutePath)
            val outDuration = validatorRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (outDuration <= 0L) {
                throw IOException("File konversi MP4 tidak memiliki durasi valid.")
            }
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            throw IOException("Validasi file konversi MP4 gagal: ${e.message}", e)
        } finally {
            try { validatorRetriever.release() } catch (_: Exception) {}
        }

        return@withContext outputFile
    }

    suspend fun exportHlsToMp4(
        context: Context,
        playlistUrl: String,
        outputFile: File,
        headers: Map<String, String>,
        onProgress: (percent: Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (outputFile.exists()) {
            outputFile.delete()
        }

        onProgress(5)

        val client = okhttp3.OkHttpClient.Builder()
            .cookieJar(com.repp.aquaticremineztools.data.manga.source.CrotPediaCookieJar(context))
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // Step 1: Fetch .m3u8 playlist
        val playlistReq = okhttp3.Request.Builder()
            .url(playlistUrl)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()

        val playlistContent = client.newCall(playlistReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("Gagal mengunduh playlist HLS (HTTP ${resp.code})")
            }
            val contentType = resp.header("Content-Type")?.lowercase() ?: ""
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                val bodySnippet = resp.body?.string()?.take(200) ?: ""
                throw IOException("Playlist HLS bukan file .m3u8 valid (Content-Type: $contentType). Info: $bodySnippet")
            }
            val bodyStr = resp.body?.string() ?: throw IOException("Playlist HLS kosong")
            if (bodyStr.trimStart().startsWith("<")) {
                throw IOException("Playlist HLS mengembalikan dokumen HTML: ${bodyStr.take(150)}")
            }
            bodyStr
        }

        // Step 2: Parse segment URLs from .m3u8
        val segmentUrls = mutableListOf<String>()
        val baseUri = java.net.URI(playlistUrl)
        playlistContent.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val resolvedSegmentUrl = try {
                    baseUri.resolve(trimmed).toString()
                } catch (_: Exception) {
                    trimmed
                }
                segmentUrls.add(resolvedSegmentUrl)
            }
        }

        if (segmentUrls.isEmpty()) {
            throw IOException("Tidak ditemukan segmen .ts di dalam playlist HLS.")
        }

        val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
        val tempTsFile = File(videosDir, "hls_temp_${System.currentTimeMillis()}.ts")

        try {
            java.io.FileOutputStream(tempTsFile, true).use { fos ->
                segmentUrls.forEachIndexed { index, segUrl ->
                    val segReq = okhttp3.Request.Builder()
                        .url(segUrl)
                        .apply { headers.forEach { (k, v) -> header(k, v) } }
                        .build()

                    client.newCall(segReq).execute().use { segResp ->
                        if (segResp.isSuccessful) {
                            segResp.body?.byteStream()?.use { input ->
                                input.copyTo(fos)
                            }
                        }
                    }

                    val percent = 5 + ((index + 1) * 45) / segmentUrls.size
                    onProgress(percent.coerceIn(5, 50))
                }
            }

            if (!tempTsFile.exists() || tempTsFile.length() == 0L) {
                throw IOException("Gagal mengunduh segmen video HLS.")
            }

            // Step 3: Convert combined .ts file to .mp4 using convertTsToMp4
            return@withContext convertTsToMp4(
                context = context,
                sourceFile = tempTsFile,
                outputFileName = outputFile.name,
                onProgress = { p ->
                    onProgress(50 + p / 2)
                }
            )
        } finally {
            if (tempTsFile.exists()) {
                tempTsFile.delete()
            }
        }
    }
}
