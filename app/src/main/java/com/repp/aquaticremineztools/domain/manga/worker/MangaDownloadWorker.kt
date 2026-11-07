package com.repp.aquaticremineztools.domain.manga.worker

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
import com.repp.aquaticremineztools.data.local.manga.MangaDatabase
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.domain.manga.PdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MangaDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = MangaDatabase.getInstance(context)
    private val dao = db.mangaDownloadDao()
    private val parser = CrotPediaParser()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val UNIQUE_WORK_NAME = "manga_download_fifo_worker"
        private const val CHANNEL_ID = "manga_download_channel"
        private const val NOTIFICATION_ID = 9012

        private val activeCalls = ConcurrentHashMap<Long, Call>()

        fun enqueueWork(context: Context) {
            val request = OneTimeWorkRequestBuilder<MangaDownloadWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }

        fun cancelWorkIfRunning(context: Context, id: Long) {
            activeCalls[id]?.cancel()
            activeCalls.remove(id)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        showForegroundNotification("Manga Downloader", "Processing download queue...")

        // Recovery on startup: reset stuck items
        dao.resetStuckDownloadingItems()

        while (true) {
            if (isStopped) {
                return@withContext Result.failure()
            }

            val currentItem = dao.getNextQueuedItem() ?: break

            val claimed = dao.claimQueuedItem(currentItem.id)
            if (claimed == 0) {
                continue
            }

            try {
                processItem(currentItem)
            } catch (e: Exception) {
                activeCalls.remove(currentItem.id)
                val checkCancelled = dao.getById(currentItem.id)
                if (checkCancelled?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                    val failedItem = currentItem.copy(
                        status = MangaDownloadEntity.STATUS_FAILED,
                        errorMessage = e.localizedMessage ?: "Download error"
                    )
                    dao.update(failedItem)
                }
            } finally {
                activeCalls.remove(currentItem.id)
            }
        }

        Result.success()
    }

    private suspend fun processItem(item: MangaDownloadEntity) {
        val pages = parser.parsePagesForChapter(item.chapterUrl)
        if (pages.isEmpty()) {
            dao.update(item.copy(status = MangaDownloadEntity.STATUS_FAILED, errorMessage = "Tidak ada gambar yang ditemukan untuk chapter ini."))
            return
        }

        val totalPages = pages.size
        val updatedTotalPagesItem = item.copy(totalPages = totalPages, status = MangaDownloadEntity.STATUS_DOWNLOADING)
        dao.update(updatedTotalPagesItem)

        val tempDir = File(applicationContext.cacheDir, "manga_temp_${item.id}_${System.currentTimeMillis()}").apply { mkdirs() }
        val downloadedFiles = mutableListOf<File>()

        try {
            var totalBytesDownloaded = 0L
            val startTime = System.currentTimeMillis()

            for (page in pages) {
                val checkStatus = dao.getById(item.id)
                if (isStopped || checkStatus?.status == MangaDownloadEntity.STATUS_CANCELLED) {
                    return
                }

                val pageFile = File(tempDir, "page_${page.pageIndex}.jpg")
                val bytes = downloadFile(item.id, page.imageUrl, pageFile)
                totalBytesDownloaded += bytes
                downloadedFiles.add(pageFile)

                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                val speedBps = if (elapsedSec > 0) (totalBytesDownloaded / elapsedSec).toLong() else 0L

                val progressItem = updatedTotalPagesItem.copy(
                    downloadedPages = page.pageIndex,
                    downloadedBytes = totalBytesDownloaded,
                    totalBytes = if (page.pageIndex > 0) (totalBytesDownloaded * totalPages / page.pageIndex) else 0L,
                    downloadSpeedBps = speedBps
                )
                dao.update(progressItem)

                showForegroundNotification(
                    "Downloading ${item.chapterTitle}",
                    "Page ${page.pageIndex}/$totalPages"
                )
            }

            val checkFinal = dao.getById(item.id)
            if (checkFinal?.status == MangaDownloadEntity.STATUS_CANCELLED) {
                return
            }

            val pdfResult = PdfBuilder.createPdfFromImages(
                applicationContext,
                downloadedFiles,
                item.outputFileName
            )

            val finalStatus = if (pdfResult.isTargetSizeAchieved) {
                MangaDownloadEntity.STATUS_COMPLETED
            } else {
                MangaDownloadEntity.STATUS_COMPLETED_WITH_WARNING
            }

            val completedItem = updatedTotalPagesItem.copy(
                status = finalStatus,
                downloadedPages = totalPages,
                downloadedBytes = totalBytesDownloaded,
                totalBytes = totalBytesDownloaded,
                pdfPath = pdfResult.pdfUriString,
                errorMessage = pdfResult.warningMessage
            )
            dao.update(completedItem)

        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun downloadFile(itemId: Long, url: String, destFile: File): Long {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Referer", "https://crotpedia.net/")
            .build()

        val call = okHttpClient.newCall(request)
        activeCalls[itemId] = call

        call.execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} pada gambar")
            val body = response.body ?: throw IllegalStateException("Respons gambar kosong")

            var bytesCopied = 0L
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                    }
                }
            }
            return bytesCopied
        }
    }

    private suspend fun showForegroundNotification(title: String, content: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Manga Download Service",
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
}
