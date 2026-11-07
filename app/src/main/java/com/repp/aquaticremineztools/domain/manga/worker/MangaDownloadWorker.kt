package com.repp.aquaticremineztools.domain.manga.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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

        fun enqueueWork(context: Context) {
            val request = OneTimeWorkRequestBuilder<MangaDownloadWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancelWorkIfRunning(context: Context, id: Long) {
            // Handled via database status change to CANCELLED
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        showForegroundNotification("Manga Downloader", "Processing download queue...")

        while (true) {
            if (isStopped) {
                return@withContext Result.failure()
            }

            val currentItem = dao.getNextQueuedItem() ?: break

            try {
                processItem(currentItem)
            } catch (e: Exception) {
                val failedItem = currentItem.copy(
                    status = MangaDownloadEntity.STATUS_FAILED,
                    errorMessage = e.localizedMessage ?: "Download error"
                )
                dao.update(failedItem)
            }
        }

        Result.success()
    }

    private suspend fun processItem(item: MangaDownloadEntity) {
        val activeItem = item.copy(status = MangaDownloadEntity.STATUS_DOWNLOADING)
        dao.update(activeItem)

        val pages = parser.parsePagesForChapter(activeItem.chapterUrl)
        if (pages.isEmpty()) {
            dao.update(activeItem.copy(status = MangaDownloadEntity.STATUS_FAILED, errorMessage = "No images found for chapter"))
            return
        }

        val totalPages = pages.size
        val updatedTotalPagesItem = activeItem.copy(totalPages = totalPages)
        dao.update(updatedTotalPagesItem)

        val tempDir = File(applicationContext.cacheDir, "manga_temp_${activeItem.id}").apply { mkdirs() }
        val downloadedFiles = mutableListOf<File>()

        var totalBytesDownloaded = 0L
        val startTime = System.currentTimeMillis()

        for (page in pages) {
            val checkStatus = dao.getById(activeItem.id)
            if (isStopped || checkStatus?.status == MangaDownloadEntity.STATUS_CANCELLED) {
                tempDir.deleteRecursively()
                return
            }

            val pageFile = File(tempDir, "page_${page.pageIndex}.jpg")
            val bytes = downloadFile(page.imageUrl, pageFile)
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
                "Downloading ${activeItem.chapterTitle}",
                "Page ${page.pageIndex}/$totalPages"
            )
        }

        // PDF Generation
        val pdfDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "AquaticManga"
        ).apply { mkdirs() }

        val pdfFile = File(pdfDir, activeItem.outputFileName)
        val pdfResult = PdfBuilder.createPdfFromImages(downloadedFiles, pdfFile)

        // Clean up temp images
        tempDir.deleteRecursively()

        // Check if cancelled
        val checkFinal = dao.getById(activeItem.id)
        if (checkFinal?.status == MangaDownloadEntity.STATUS_CANCELLED) {
            pdfFile.delete()
            return
        }

        val completedItem = updatedTotalPagesItem.copy(
            status = MangaDownloadEntity.STATUS_COMPLETED,
            downloadedPages = totalPages,
            downloadedBytes = totalBytesDownloaded,
            totalBytes = totalBytesDownloaded,
            pdfPath = pdfResult.pdfFile.absolutePath,
            errorMessage = pdfResult.warningMessage
        )
        dao.update(completedItem)
    }

    private fun downloadFile(url: String, destFile: File): Long {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Referer", "https://crotpedia.net/")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Image HTTP ${response.code}")
            val body = response.body ?: throw IllegalStateException("Empty image body")

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
