package com.repp.aquaticremineztools.domain.manga.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.data.local.manga.MangaDatabase
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.local.manga.MangaResultEntity
import com.repp.aquaticremineztools.data.manga.source.CrotPediaCookieJar
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.data.manga.source.MangaAuthRequiredException
import com.repp.aquaticremineztools.domain.manga.PdfBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MangaDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = MangaDatabase.getInstance(context)
    private val dao = db.mangaDownloadDao()
    private val resultDao = db.mangaResultDao()

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(CrotPediaCookieJar(context))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val parser = CrotPediaParser(okHttpClient)

    companion object {
        private const val UNIQUE_WORK_NAME = "manga_download_fifo_worker"
        private const val CHANNEL_ID = "manga_download_channel"
        private const val NOTIFICATION_ID = 9012

        private val activeCalls = ConcurrentHashMap<Long, Call>()

        fun enqueueWork(context: Context) {
            try {
                val request = OneTimeWorkRequestBuilder<MangaDownloadWorker>().build()
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
        showForegroundNotification("Manga Downloader", "Processing download queue...")

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
                if (checkCancelled?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                    dao.resetItemToQueued(currentItem.id)
                }
                throw e
            } catch (e: MangaAuthRequiredException) {
                activeCalls.remove(currentItem.id)
                CrotPediaSessionStore.clearSession(applicationContext)
                dao.markFailedIfDownloading(currentItem.id, "SESSION EXPIRED")
                return@withContext Result.failure()
            } catch (e: Exception) {
                activeCalls.remove(currentItem.id)
                val checkItem = dao.getById(currentItem.id)
                if (checkItem?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                    dao.markFailedIfDownloading(currentItem.id, e.localizedMessage ?: "Download error")
                }
            } finally {
                activeCalls.remove(currentItem.id)
            }
        }

        Result.success()
    }

    private suspend fun processItem(item: MangaDownloadEntity) {
        val checkPre = dao.getById(item.id)
        if (isStopped || checkPre?.status == MangaDownloadEntity.STATUS_CANCELLED) {
            if (isStopped && checkPre?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                dao.resetItemToQueued(item.id)
            }
            return
        }

        if (item.stage == MangaDownloadEntity.STAGE_COMPRESSING_PDF) {
            processCompressionItem(item)
            return
        }

        dao.updateHeartbeat(item.id)

        var pages = try {
            parser.parsePagesForChapter(item.chapterUrl)
        } catch (e: Exception) {
            if (e is MangaAuthRequiredException) throw e
            throw IOException("Gagal memuat halaman chapter: ${e.message}", e)
        }

        if (pages.isEmpty()) {
            dao.markFailedIfDownloading(item.id, "Tidak ada gambar yang ditemukan untuk chapter ini.")
            return
        }

        val totalPages = pages.size
        val updatedRows = dao.updateProgressIfDownloading(
            id = item.id,
            totalPages = totalPages,
            downloadedPages = 0,
            bytes = 0L,
            totalBytes = 0L,
            speed = 0L,
            stage = MangaDownloadEntity.STAGE_DOWNLOADING_IMAGES,
            stageProgress = 0
        )
        if (updatedRows == 0) {
            val postCheck = dao.getById(item.id)
            if (isStopped && postCheck?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                dao.resetItemToQueued(item.id)
            }
            return
        }

        val tempDir = File(applicationContext.cacheDir, "manga_temp_${item.id}_${System.currentTimeMillis()}").apply { mkdirs() }
        val downloadedFiles = mutableListOf<File>()

        try {
            var totalBytesDownloaded = 0L
            val startTime = System.currentTimeMillis()

            for (page in pages) {
                val checkStatus = dao.getById(item.id)
                if (isStopped || checkStatus?.status == MangaDownloadEntity.STATUS_CANCELLED) {
                    if (isStopped && checkStatus?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                        dao.resetItemToQueued(item.id)
                    }
                    return
                }

                dao.updateHeartbeat(item.id)

                val pageFile = File(tempDir, "page_${page.pageIndex}.jpg")
                var bytes = 0L
                var downloadSuccess = false

                for (attempt in 1..2) {
                    try {
                        val activePage = pages.find { it.pageIndex == page.pageIndex } ?: page
                        bytes = downloadFile(item.id, activePage.imageUrl, pageFile)
                        validateDownloadedImage(pageFile)
                        downloadSuccess = true
                        break
                    } catch (e: Exception) {
                        if (e is MangaAuthRequiredException) throw e
                        if (attempt >= 2) {
                            throw IOException("Gagal mengunduh halaman ${page.pageIndex} setelah retry: ${e.message}", e)
                        }
                        try {
                            val refreshedPages = parser.parsePagesForChapter(item.chapterUrl)
                            val refreshedPage = refreshedPages.find { it.pageIndex == page.pageIndex }
                            if (refreshedPage != null) {
                                val mutablePages = pages.toMutableList()
                                val idx = mutablePages.indexOfFirst { it.pageIndex == page.pageIndex }
                                if (idx != -1) {
                                    mutablePages[idx] = refreshedPage
                                } else {
                                    mutablePages.add(refreshedPage)
                                }
                                pages = mutablePages.sortedBy { it.pageIndex }
                            }
                        } catch (_: Exception) {}
                    }
                }

                if (!downloadSuccess) {
                    throw IOException("Gagal mengunduh halaman ${page.pageIndex}")
                }

                totalBytesDownloaded += bytes
                downloadedFiles.add(pageFile)

                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                val speedBps = if (elapsedSec > 0) (totalBytesDownloaded / elapsedSec).toLong() else 0L
                val stagePercent = if (totalPages > 0) (page.pageIndex * 100) / totalPages else 0

                val progressUpdated = dao.updateProgressIfDownloading(
                    id = item.id,
                    totalPages = totalPages,
                    downloadedPages = page.pageIndex,
                    bytes = totalBytesDownloaded,
                    totalBytes = if (page.pageIndex > 0) (totalBytesDownloaded * totalPages / page.pageIndex) else totalBytesDownloaded,
                    speed = speedBps,
                    stage = MangaDownloadEntity.STAGE_DOWNLOADING_IMAGES,
                    stageProgress = stagePercent.coerceIn(0, 100)
                )
                if (progressUpdated == 0) {
                    val postCheck = dao.getById(item.id)
                    if (isStopped && postCheck?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                        dao.resetItemToQueued(item.id)
                    }
                    return
                }

                showForegroundNotification(
                    "Downloading ${item.chapterTitle}",
                    "Page ${page.pageIndex}/$totalPages"
                )
            }

            val checkFinal = dao.getById(item.id)
            if (isStopped || checkFinal?.status == MangaDownloadEntity.STATUS_CANCELLED) {
                if (isStopped && checkFinal?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                    dao.resetItemToQueued(item.id)
                }
                return
            }

            // Transition to Stage 2: Building & Validating PDF (without automatic compression per Part 2)
            dao.updateStageProgress(item.id, MangaDownloadEntity.STAGE_BUILDING_PDF, 0)

            val pdfResult = PdfBuilder.createPdfFromImages(
                context = applicationContext,
                imageFiles = downloadedFiles,
                outputFileName = item.outputFileName,
                expectedPages = totalPages,
                isCancelledCheck = {
                    isStopped || dao.getById(item.id)?.status == MangaDownloadEntity.STATUS_CANCELLED
                },
                onProgress = { stage, percent ->
                    dao.updateStageProgress(item.id, stage, percent.coerceIn(0, 100))
                }
            )

            if (pdfResult.pdfUriString.isBlank() || pdfResult.pageCount != totalPages) {
                throw IllegalStateException("Validasi PDF gagal: URI kosong atau jumlah halaman tidak cocok.")
            }

            val recheckCancelled = dao.getById(item.id)
            if (isStopped || recheckCancelled?.status == MangaDownloadEntity.STATUS_CANCELLED) {
                try {
                    applicationContext.contentResolver.delete(android.net.Uri.parse(pdfResult.pdfUriString), null, null)
                } catch (_: Exception) {}
                if (isStopped && recheckCancelled?.status != MangaDownloadEntity.STATUS_CANCELLED) {
                    dao.resetItemToQueued(item.id)
                }
                return
            }

            // Insert into Result table with status NOT_COMPLETED
            val resultEntity = MangaResultEntity(
                seriesTitle = item.seriesTitle,
                chapterTitle = item.chapterTitle,
                chapterUrl = item.chapterUrl,
                pdfPath = pdfResult.pdfUriString,
                sizeBytes = pdfResult.sizeBytes,
                pageCount = totalPages,
                status = MangaResultEntity.STATUS_NOT_COMPLETED,
                errorMessage = pdfResult.warningMessage
            )
            resultDao.insert(resultEntity)

            // Mark download item as completed so it leaves active queue
            dao.markCompletedIfDownloading(
                id = item.id,
                finalStatus = MangaDownloadEntity.STATUS_COMPLETED,
                totalPages = totalPages,
                downloadedPages = totalPages,
                bytes = pdfResult.sizeBytes,
                totalBytes = pdfResult.sizeBytes,
                pdfPath = pdfResult.pdfUriString,
                warningMessage = pdfResult.warningMessage
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun processCompressionItem(item: MangaDownloadEntity) {
        dao.updateStageProgress(item.id, MangaDownloadEntity.STAGE_BUILDING_PDF, 0)
        showForegroundNotification("Compressing PDF", item.chapterTitle)

        val resultId = item.id // or passed via chapterUrl/outputFileName
        // Let's find result by chapterUrl or outputFileName if needed, or pass resultId in chapterUrl or similar.
        // Even better, let's store resultId or query latest result for this chapterUrl.
        val resultEntity = resultDao.getAllResultsLiveData().value?.find { it.chapterUrl == item.chapterUrl }
            ?: resultDao.getAllResultsLiveData().value?.firstOrNull()

        if (resultEntity == null) {
            dao.markFailedIfDownloading(item.id, "Result PDF tidak ditemukan untuk kompresi.")
            return
        }

        resultDao.updateStatus(resultEntity.id, MangaResultEntity.STATUS_COMPRESSING)

        val sourceUri = Uri.parse(resultEntity.pdfPath)
        val sourceFile = File(applicationContext.cacheDir, "source_compress_${System.currentTimeMillis()}.pdf")
        try {
            applicationContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(sourceFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Gagal membaca file PDF sumber.")

            val compressResult = PdfBuilder.compressExistingPdf(
                context = applicationContext,
                sourcePdfFile = sourceFile,
                outputFileName = item.outputFileName,
                quality = 75,
                maxDimension = 1400,
                maxSizeBytes = 20 * 1024 * 1024L,
                isCancelledCheck = {
                    isStopped || dao.getById(item.id)?.status == MangaDownloadEntity.STATUS_CANCELLED
                },
                onProgress = { stage, percent ->
                    dao.updateStageProgress(item.id, stage, percent.coerceIn(0, 100))
                }
            )

            resultDao.updatePdfResult(
                id = resultEntity.id,
                pdfPath = compressResult.pdfUriString,
                sizeBytes = compressResult.sizeBytes,
                status = MangaResultEntity.STATUS_COMPLETED,
                errorMessage = compressResult.warningMessage
            )

            dao.markCompletedIfDownloading(
                id = item.id,
                finalStatus = MangaDownloadEntity.STATUS_COMPLETED,
                totalPages = resultEntity.pageCount,
                downloadedPages = resultEntity.pageCount,
                bytes = compressResult.sizeBytes,
                totalBytes = compressResult.sizeBytes,
                pdfPath = compressResult.pdfUriString,
                warningMessage = compressResult.warningMessage
            )
        } catch (e: Exception) {
            resultDao.updateStatus(resultEntity.id, MangaResultEntity.STATUS_FAILED, e.localizedMessage ?: "Kompresi gagal")
            dao.markFailedIfDownloading(item.id, e.localizedMessage ?: "Kompresi gagal")
            throw e
        } finally {
            if (sourceFile.exists()) {
                sourceFile.delete()
            }
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
            if (response.code == 401) {
                throw MangaAuthRequiredException("Sesi login kedaluwarsa (HTTP 401).")
            }
            if (response.code == 403) {
                throw IOException("Akses gambar ditolak (HTTP 403). URL mungkin kedaluwarsa.")
            }
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} pada gambar")
            val body = response.body ?: throw IOException("Respons gambar kosong")

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

    private fun validateDownloadedImage(file: File) {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("File gambar kosong atau tidak ditemukan.")
        }
        val sampleBytes = ByteArray(100)
        try {
            file.inputStream().use { it.read(sampleBytes) }
            val sampleStr = String(sampleBytes, Charsets.UTF_8).lowercase()
            if (sampleStr.contains("<!doctype") || sampleStr.contains("<html") || sampleStr.contains("<body") || sampleStr.contains("access denied")) {
                throw IOException("File yang diunduh adalah halaman error HTML, bukan gambar.")
            }
        } catch (e: IOException) {
            throw e
        } catch (_: Exception) {}

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw IOException("File gambar korup atau gagal didecode.")
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
