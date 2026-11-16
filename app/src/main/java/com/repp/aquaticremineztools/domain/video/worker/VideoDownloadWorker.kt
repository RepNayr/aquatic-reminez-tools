package com.repp.aquaticremineztools.domain.video.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.video.VideoDatabase
import com.repp.aquaticremineztools.data.local.video.VideoDownloadEntity
import com.repp.aquaticremineztools.data.local.video.VideoResultEntity
import com.repp.aquaticremineztools.data.video.model.VideoQuality
import com.repp.aquaticremineztools.data.video.model.VideoSource
import com.repp.aquaticremineztools.data.video.source.HentaiCopParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import com.repp.aquaticremineztools.data.video.network.HentaiCopCookieJar

class VideoDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = VideoDatabase.getInstance(context)
    private val dao = db.videoDownloadDao()
    private val resultDao = db.videoResultDao()

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(HentaiCopCookieJar(context))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()
    private val parser = HentaiCopParser(context, okHttpClient)

    companion object {
        private const val UNIQUE_WORK_NAME = "video_download_fifo_worker"
        private const val CHANNEL_ID = "video_download_channel"
        private const val NOTIFICATION_ID = 9013

        private val activeCalls = ConcurrentHashMap<Long, Call>()

        fun enqueueWork(context: Context) {
            try {
                val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>().build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                )
            } catch (_: Exception) {
                // Fail-safe if WorkManager is not initialized in unit tests
            }
        }

        fun cancelWorkIfRunning(context: Context, id: Long) {
            activeCalls[id]?.cancel()
            activeCalls.remove(id)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        showForegroundNotification("Video Downloader", "Processing video queue...")

        val stuckThreshold = System.currentTimeMillis() - 300000L
        dao.resetStuckDownloadingItems(stuckThreshold)

        while (true) {
            if (isStopped) {
                return@withContext Result.success()
            }

            val currentItem = dao.getNextQueuedItem() ?: break

            val claimed = dao.claimQueuedItem(currentItem.id)
            if (claimed == 0) {
                continue
            }

            try {
                processItem(currentItem)
            } catch (e: CancellationException) {
                activeCalls.remove(currentItem.id)
                val checkCancelled = dao.getById(currentItem.id)
                if (checkCancelled?.status != VideoDownloadEntity.STATUS_CANCELLED) {
                    dao.resetItemToQueued(currentItem.id)
                }
                throw e
            } catch (e: Exception) {
                activeCalls.remove(currentItem.id)
                val checkItem = dao.getById(currentItem.id)
                if (checkItem?.status != VideoDownloadEntity.STATUS_CANCELLED) {
                    dao.markFailedIfDownloading(currentItem.id, e.localizedMessage ?: "Download error")
                }
            } finally {
                activeCalls.remove(currentItem.id)
            }
        }

        Result.success()
    }

    private suspend fun processItem(item: VideoDownloadEntity) {
        val checkPre = dao.getById(item.id)
        if (isStopped || checkPre?.status == VideoDownloadEntity.STATUS_CANCELLED) {
            if (isStopped && checkPre?.status != VideoDownloadEntity.STATUS_CANCELLED) {
                dao.resetItemToQueued(item.id)
            }
            return
        }

        if (item.stage == VideoDownloadEntity.STAGE_COMPRESSING_VIDEO) {
            processCompressionItem(item)
            return
        }

        dao.updateHeartbeat(item.id)

        // Resolve direct media URL from download page URL
        val source = VideoSource(
            serverId = item.serverId,
            serverName = "HentaiCop",
            quality = VideoQuality(label = item.qualityLabel),
            downloadPageUrl = item.downloadPageUrl,
            refererHeader = item.episodeUrl
        )

        val resolvedSource = try {
            dao.updateStage(item.id, VideoDownloadEntity.STAGE_RESOLVING_DOWNLOAD_PAGE)
            parser.resolveDirectMediaSource(source)
        } catch (e: Exception) {
            throw IOException("Gagal mendapatkan link media video: ${e.message}", e)
        }

        dao.updateStage(item.id, VideoDownloadEntity.STAGE_MEDIA_URL_RESOLVED)

        val videosDir = File(applicationContext.filesDir, "videos").apply { mkdirs() }
        val tempTsFile = File(videosDir, "video_temp_${item.id}.ts")

        try {
            downloadStreamingMedia(item, resolvedSource, tempTsFile)

            dao.updateStage(item.id, VideoDownloadEntity.STAGE_STREAMING_COMPLETED)

            val checkFinal = dao.getById(item.id)
            if (isStopped || checkFinal?.status == VideoDownloadEntity.STATUS_CANCELLED) {
                if (isStopped && checkFinal?.status != VideoDownloadEntity.STATUS_CANCELLED) {
                    dao.resetItemToQueued(item.id)
                }
                return
            }

            if (!tempTsFile.exists() || tempTsFile.length() == 0L) {
                throw IOException("Hasil unduhan video stream kosong atau tidak ditemukan.")
            }

            dao.updateStage(item.id, VideoDownloadEntity.STAGE_CONVERTING_VIDEO)
            showForegroundNotification("Converting Video", item.episodeTitle)

            val convertedFile = com.repp.aquaticremineztools.domain.video.VideoCompressor.convertTsToMp4(
                context = applicationContext,
                sourceFile = tempTsFile,
                outputFileName = item.outputFileName,
                onProgress = { percent ->
                    kotlinx.coroutines.runBlocking {
                        dao.updateProgressIfDownloading(
                            id = item.id,
                            bytes = tempTsFile.length(),
                            totalBytes = item.totalBytes.coerceAtLeast(tempTsFile.length()),
                            speed = 0L,
                            percent = percent
                        )
                    }
                }
            )

            val finalSizeBytes = convertedFile.length()
            dao.updateStage(item.id, VideoDownloadEntity.STAGE_MEDIA_VALIDATION)
            validateDownloadedVideoFile(convertedFile, item.totalBytes)

            dao.updateStage(item.id, VideoDownloadEntity.STAGE_RESULT_SAVED)
            // Insert into VideoResultEntity
            val resultEntity = VideoResultEntity(
                seriesTitle = item.seriesTitle,
                episodeTitle = item.episodeTitle,
                episodeUrl = item.episodeUrl,
                videoPath = convertedFile.absolutePath,
                fileFormat = "MP4",
                resolutionLabel = item.qualityLabel,
                sizeBytes = finalSizeBytes,
                originalSizeBytes = finalSizeBytes,
                durationFormatted = "",
                thumbnailUrl = "",
                status = VideoResultEntity.STATUS_COMPLETED
            )
            resultDao.insert(resultEntity)

            dao.markCompletedIfDownloading(
                id = item.id,
                finalStatus = VideoDownloadEntity.STATUS_COMPLETED,
                bytes = finalSizeBytes,
                totalBytes = finalSizeBytes,
                videoPath = convertedFile.absolutePath
            )
        } finally {
            if (tempTsFile.exists()) {
                tempTsFile.delete()
            }
        }
    }

    private suspend fun processCompressionItem(item: VideoDownloadEntity) {
        showForegroundNotification("Compressing Video", item.episodeTitle)
        val sourcePath = item.videoPath
        val resultEntity = resultDao.getByEpisodeUrl(item.episodeUrl)

        if (sourcePath.isNullOrBlank()) {
            dao.markFailedIfDownloading(item.id, "File video sumber tidak ditemukan untuk kompresi.")
            return
        }

        val targetSizeBytes = if (item.totalBytes > 0) item.totalBytes else 50 * 1024 * 1024L
        val sourceFile = File(sourcePath)

        val targetResultId = resultEntity?.id
        if (targetResultId != null) {
            resultDao.updateStatus(targetResultId, VideoResultEntity.STATUS_COMPRESSING)
        }

        try {
            val compressedFile = com.repp.aquaticremineztools.domain.video.VideoCompressor.compressVideo(
                context = applicationContext,
                sourceFile = sourceFile,
                outputFileName = item.outputFileName,
                targetSizeBytes = targetSizeBytes,
                onProgress = { percent ->
                    kotlinx.coroutines.runBlocking {
                        dao.updateProgressIfDownloading(
                            id = item.id,
                            bytes = (targetSizeBytes * percent) / 100,
                            totalBytes = targetSizeBytes,
                            speed = 0L,
                            percent = percent
                        )
                    }
                }
            )

            if (targetResultId != null) {
                resultDao.updateVideoResult(
                    id = targetResultId,
                    videoPath = compressedFile.absolutePath,
                    sizeBytes = compressedFile.length(),
                    oldSizeBytes = resultEntity.sizeBytes,
                    status = VideoResultEntity.STATUS_COMPLETED
                )
            }

            dao.markCompletedIfDownloading(
                id = item.id,
                finalStatus = VideoDownloadEntity.STATUS_COMPLETED,
                bytes = compressedFile.length(),
                totalBytes = compressedFile.length(),
                videoPath = compressedFile.absolutePath
            )
        } catch (e: Exception) {
            if (targetResultId != null) {
                resultDao.updateStatus(targetResultId, VideoResultEntity.STATUS_FAILED, e.localizedMessage ?: "Kompresi gagal")
            }
            dao.markFailedIfDownloading(item.id, e.localizedMessage ?: "Kompresi gagal")
            throw e
        }
    }

    private suspend fun downloadStreamingMedia(
        item: VideoDownloadEntity,
        resolvedSource: com.repp.aquaticremineztools.data.video.model.ResolvedVideoSource,
        destFile: File
    ) {
        val mediaUrl = resolvedSource.mediaUrl
        if (mediaUrl.contains(".m3u8") || mediaUrl.contains("/hls/")) {
            downloadHlsPlaylistMedia(item, resolvedSource, destFile)
            return
        }

        var existingLength = if (destFile.exists()) destFile.length() else 0L

        dao.updateStage(item.id, VideoDownloadEntity.STAGE_OPENING_MEDIA_REQUEST)

        val userAgent = resolvedSource.userAgent.ifBlank { HentaiCopParser.USER_AGENT }
        val referer = resolvedSource.referer.ifBlank { item.episodeUrl }

        val reqBuilder = Request.Builder()
            .url(mediaUrl)
            .header("User-Agent", userAgent)
            .header("Referer", referer)

        if (existingLength > 0L) {
            reqBuilder.header("Range", "bytes=$existingLength-")
        }

        val request = reqBuilder.build()
        val call = okHttpClient.newCall(request)
        activeCalls[item.id] = call

        call.execute().use { response ->
            dao.updateStage(item.id, VideoDownloadEntity.STAGE_VALIDATING_HTTP_RESPONSE)
            if (response.code == 401) {
                throw IOException("[HTTP_UNAUTHORIZED] Server menolak akses (HTTP 401 Unauthorized)")
            }
            if (response.code == 403) {
                throw IOException("[HTTP_FORBIDDEN] Server menolak akses (HTTP 403 Forbidden)")
            }
            if (response.code == 404) {
                throw IOException("[HTTP_NOT_FOUND] File video tidak ditemukan pada server (HTTP 404 Not Found)")
            }
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("[HTTP_${response.code}] Server mengembalikan status HTTP ${response.code} saat mengunduh video")
            }

            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val host = response.request.url.host
            if (contentType.contains("text/html")) {
                val bodySnippet = response.body?.string()?.take(200) ?: ""
                throw IOException("[HTML_RESPONSE_INSTEAD_OF_MEDIA] Respons server bukan file video valid (Content-Type: $contentType, Host: $host). Snippet: $bodySnippet")
            }
            if (contentType.contains("application/json")) {
                val bodySnippet = response.body?.string()?.take(200) ?: ""
                throw IOException("[JSON_RESPONSE_INSTEAD_OF_MEDIA] Respons server berupa JSON (Content-Type: $contentType). Snippet: $bodySnippet")
            }

            val body = response.body ?: throw IOException("[EMPTY_MEDIA_RESPONSE] Body respons video kosong")
            val isPartialContent = response.code == 206

            if (!isPartialContent && existingLength > 0L) {
                // Server ignored Range header or reset; restart download from byte 0
                existingLength = 0L
                destFile.delete()
            }

            dao.updateStage(item.id, VideoDownloadEntity.STAGE_STREAMING_STARTED)

            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) (existingLength + contentLength) else item.totalBytes

            val startTime = System.currentTimeMillis()
            var currentBytes = existingLength

            RandomAccessFile(destFile, "rw").use { raf ->
                raf.seek(existingLength)

                val buffer = ByteArray(65536) // 64KB buffer for high video throughput
                val inputStream = body.byteStream()
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val checkStatus = dao.getById(item.id)
                    if (isStopped || checkStatus?.status == VideoDownloadEntity.STATUS_CANCELLED) {
                        return
                    }

                    raf.write(buffer, 0, bytesRead)
                    currentBytes += bytesRead

                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                    val speedBps = if (elapsedSec > 0) ((currentBytes - existingLength) / elapsedSec).toLong() else 0L
                    val percent = if (totalBytes > 0) ((currentBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

                    dao.updateProgressIfDownloading(
                        id = item.id,
                        bytes = currentBytes,
                        totalBytes = totalBytes,
                        speed = speedBps,
                        percent = percent
                    )

                    showForegroundNotification(
                        "Downloading ${item.episodeTitle}",
                        "$percent% (${currentBytes / (1024 * 1024)}MB / ${totalBytes / (1024 * 1024)}MB)"
                    )
                }
            }
        }
    }

    private suspend fun downloadHlsPlaylistMedia(
        item: VideoDownloadEntity,
        resolvedSource: com.repp.aquaticremineztools.data.video.model.ResolvedVideoSource,
        destFile: File
    ) {
        dao.updateStage(item.id, VideoDownloadEntity.STAGE_STREAMING_STARTED)
        showForegroundNotification("Downloading HLS ${item.episodeTitle}", "Processing HLS stream (.m3u8)...")

        val userAgent = resolvedSource.userAgent.ifBlank { HentaiCopParser.USER_AGENT }
        val referer = resolvedSource.referer.ifBlank { item.episodeUrl }

        val headers = mapOf(
            "User-Agent" to userAgent,
            "Referer" to referer
        )

        com.repp.aquaticremineztools.domain.video.VideoCompressor.exportHlsToMp4(
            context = applicationContext,
            playlistUrl = resolvedSource.mediaUrl,
            outputFile = destFile,
            headers = headers,
            onProgress = { percent: Int ->
                kotlinx.coroutines.runBlocking {
                    dao.updateProgressIfDownloading(
                        id = item.id,
                        bytes = (item.totalBytes * percent) / 100,
                        totalBytes = item.totalBytes.coerceAtLeast(1L),
                        speed = 0L,
                        percent = percent
                    )
                }
            }
        )
    }

    private suspend fun showForegroundNotification(title: String, content: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Download Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForeground(ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
        } else {
            setForeground(ForegroundInfo(NOTIFICATION_ID, notification))
        }
    }

    private fun validateDownloadedVideoFile(file: File, expectedSizeBytes: Long) {
        if (!file.exists() || file.length() < 1024L) {
            throw IOException("File video kosong atau berukuran kurang dari 1 KB.")
        }

        val sampleBytes = ByteArray(1024)
        try {
            file.inputStream().use { it.read(sampleBytes) }
            val sampleStr = String(sampleBytes, Charsets.UTF_8).lowercase()
            if (sampleStr.contains("<!doctype") || sampleStr.contains("<html") || sampleStr.contains("<body") || sampleStr.contains("adblock")) {
                throw IOException("File yang diunduh adalah halaman HTML/AdBlock error, bukan file video.")
            }
        } catch (e: IOException) {
            throw e
        } catch (_: Exception) {}

        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            retriever.release()
            if (durationMs <= 0L && file.length() < 100_000L) {
                throw IOException("File video terlalu kecil atau tidak memiliki durasi valid.")
            }
        } catch (_: Exception) {
            if (file.length() < 100_000L) {
                throw IOException("File unduhan terlalu kecil (${file.length()} bytes).")
            }
        }
    }
}
