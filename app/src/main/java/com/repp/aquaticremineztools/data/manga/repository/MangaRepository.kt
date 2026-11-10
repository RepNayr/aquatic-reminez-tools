package com.repp.aquaticremineztools.data.manga.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.data.local.manga.MangaDatabase
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.local.manga.MangaResultEntity
import com.repp.aquaticremineztools.data.manga.model.MangaChapter
import com.repp.aquaticremineztools.data.manga.model.MangaSeries
import com.repp.aquaticremineztools.data.manga.source.CrotPediaCookieJar
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.domain.manga.worker.MangaDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class MangaRepository(
    context: Context,
    private val db: MangaDatabase = MangaDatabase.getInstance(context)
) {

    private val dao = db.mangaDownloadDao()
    private val resultDao = db.mangaResultDao()
    private val appContext = context.applicationContext

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(CrotPediaCookieJar(appContext))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    private val parser = CrotPediaParser(okHttpClient)

    suspend fun analyzeUrl(url: String): MangaSeries = withContext(Dispatchers.IO) {
        parser.parseUrl(url)
    }

    suspend fun parseSeries(url: String): MangaSeries = withContext(Dispatchers.IO) {
        parser.parseUrl(url)
    }

    suspend fun parsePagesForChapter(chapterUrl: String): List<com.repp.aquaticremineztools.data.manga.model.MangaPage> = withContext(Dispatchers.IO) {
        parser.parsePagesForChapter(chapterUrl)
    }

    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        if (!CrotPediaSessionStore.isLoggedIn(appContext)) {
            return@withContext false
        }

        val request = Request.Builder()
            .url("https://crotpedia.net/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Referer", "https://crotpedia.net/")
            .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Android\"")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .header("Upgrade-Insecure-Requests", "1")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    return@withContext false
                }
                val html = response.body?.string() ?: ""
                val finalUrl = response.request.url.toString()
                val isLoginPage = finalUrl.contains("/login") || finalUrl.contains("/signin")
                val hasLoginForm = html.contains("form[action*=/login]") || html.contains("input[name=log]") || html.contains("id=\"loginform\"")

                if (isLoginPage || hasLoginForm) {
                    return@withContext false
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            throw IOException("Network connection failed during session validation", e)
        }
    }

    fun logout() {
        CrotPediaSessionStore.clearSession(appContext)
    }

    private fun canonicalUrl(url: String): String {
        val clean = url.trim()
        if (clean.isEmpty()) return clean
        val queryIndex = clean.indexOf('?')
        val hashIndex = clean.indexOf('#')
        val cutIndex = when {
            queryIndex != -1 && hashIndex != -1 -> minOf(queryIndex, hashIndex)
            queryIndex != -1 -> queryIndex
            hashIndex != -1 -> hashIndex
            else -> -1
        }
        val mainPath = if (cutIndex != -1) clean.substring(0, cutIndex) else clean
        val queryAndHash = if (cutIndex != -1) clean.substring(cutIndex) else ""

        val normalizedPath = if (mainPath.endsWith("/") && mainPath.length > 1 && !mainPath.endsWith("://")) {
            mainPath.dropLast(1)
        } else {
            mainPath
        }
        return normalizedPath + queryAndHash
    }

    private fun getUrlVariants(url: String): Pair<String, String> {
        val canonical = canonicalUrl(url)
        val queryIndex = canonical.indexOf('?')
        val hashIndex = canonical.indexOf('#')
        val cutIndex = when {
            queryIndex != -1 && hashIndex != -1 -> minOf(queryIndex, hashIndex)
            queryIndex != -1 -> queryIndex
            hashIndex != -1 -> hashIndex
            else -> -1
        }
        val mainPath = if (cutIndex != -1) canonical.substring(0, cutIndex) else canonical
        val queryAndHash = if (cutIndex != -1) canonical.substring(cutIndex) else ""

        val pathWithoutSlash = if (mainPath.endsWith("/") && mainPath.length > 1 && !mainPath.endsWith("://")) {
            mainPath.dropLast(1)
        } else {
            mainPath
        }
        val pathWithSlash = "$pathWithoutSlash/"

        return (pathWithoutSlash + queryAndHash) to (pathWithSlash + queryAndHash)
    }

    suspend fun enqueueChapter(seriesTitle: String, chapter: MangaChapter): Boolean {
        val cleanUrl = chapter.chapterUrl.trim()
        val (urlWithoutSlash, urlWithSlash) = getUrlVariants(cleanUrl)
        val canonical = urlWithoutSlash
        val sanitizeTitle = sanitizeFileName("${seriesTitle}_${chapter.title}.pdf")

        return withContext(Dispatchers.IO) {
            db.withTransaction {
                val existing = dao.getByUrl(cleanUrl, urlWithoutSlash, urlWithSlash)
                if (existing != null) {
                    when (existing.status) {
                        MangaDownloadEntity.STATUS_QUEUED,
                        MangaDownloadEntity.STATUS_DOWNLOADING,
                        MangaDownloadEntity.STATUS_COMPLETED,
                        MangaDownloadEntity.STATUS_COMPLETED_WITH_WARNING -> {
                            false
                        }
                        MangaDownloadEntity.STATUS_FAILED,
                        MangaDownloadEntity.STATUS_CANCELLED -> {
                            val updated = existing.copy(
                                seriesTitle = seriesTitle,
                                chapterTitle = chapter.title,
                                chapterUrl = canonical,
                                outputFileName = sanitizeTitle,
                                totalPages = if (chapter.pageCount > 0) chapter.pageCount else existing.totalPages,
                                downloadedPages = 0,
                                totalBytes = 0L,
                                downloadedBytes = 0L,
                                downloadSpeedBps = 0L,
                                status = MangaDownloadEntity.STATUS_QUEUED,
                                timestamp = System.currentTimeMillis(),
                                lastHeartbeatAt = 0L,
                                pdfPath = null,
                                errorMessage = null
                            )
                            dao.update(updated)
                            true
                        }
                        else -> false
                    }
                } else {
                    val entity = MangaDownloadEntity(
                        seriesTitle = seriesTitle,
                        chapterTitle = chapter.title,
                        chapterUrl = canonical,
                        outputFileName = sanitizeTitle,
                        totalPages = chapter.pageCount,
                        status = MangaDownloadEntity.STATUS_QUEUED,
                        timestamp = System.currentTimeMillis(),
                        lastHeartbeatAt = 0L
                    )
                    dao.insert(entity)
                    true
                }
            }.also { enqueued ->
                if (enqueued) {
                    MangaDownloadWorker.enqueueWork(appContext)
                }
            }
        }
    }

    suspend fun enqueueAllChapters(seriesTitle: String, chapters: List<MangaChapter>): Int {
        var addedCount = 0
        chapters.forEach { chapter ->
            val success = enqueueChapter(seriesTitle, chapter)
            if (success) addedCount++
        }
        return addedCount
    }

    suspend fun cancelDownload(id: Long) {
        dao.cancelDownload(id)
        MangaDownloadWorker.cancelWorkIfRunning(appContext, id)
    }

    suspend fun retryDownload(entity: MangaDownloadEntity) {
        val chapter = MangaChapter(
            title = entity.chapterTitle,
            chapterUrl = entity.chapterUrl,
            pageCount = entity.totalPages,
            orderIndex = 0
        )
        enqueueChapter(entity.seriesTitle, chapter)
    }

    fun getActiveAndQueuedLiveData(): LiveData<List<MangaDownloadEntity>> {
        return dao.getActiveAndQueuedLiveData()
    }

    fun getCompletedDownloadsLiveData(): LiveData<List<MangaDownloadEntity>> {
        return dao.getCompletedDownloadsLiveData()
    }

    suspend fun deleteDownload(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    fun getResultsLiveData(): LiveData<List<MangaResultEntity>> {
        return resultDao.getAllResultsLiveData()
    }

    suspend fun deleteResult(id: Long) = withContext(Dispatchers.IO) {
        resultDao.deleteById(id)
    }

    suspend fun clearAllResults() = withContext(Dispatchers.IO) {
        resultDao.deleteAllResults()
    }

    suspend fun compressResult(result: MangaResultEntity, targetSizeBytes: Long = 20 * 1024 * 1024L) = withContext(Dispatchers.IO) {
        val sanitizeTitle = sanitizeFileName("compressed_${result.seriesTitle}_${result.chapterTitle}.pdf")
        val entity = MangaDownloadEntity(
            seriesTitle = result.seriesTitle,
            chapterTitle = result.chapterTitle,
            chapterUrl = result.chapterUrl,
            outputFileName = sanitizeTitle,
            totalPages = result.pageCount,
            totalBytes = targetSizeBytes,
            status = MangaDownloadEntity.STATUS_QUEUED,
            stage = MangaDownloadEntity.STAGE_COMPRESSING_PDF,
            pdfPath = result.pdfPath,
            timestamp = System.currentTimeMillis()
        )
        dao.insert(entity)
        MangaDownloadWorker.enqueueWork(appContext)
    }

    fun getActiveAndQueuedCountLiveData(): LiveData<Int> {
        return dao.getActiveAndQueuedCountLiveData()
    }

    fun getAllDownloadsLiveData(): LiveData<List<MangaDownloadEntity>> {
        return dao.getAllLiveData()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
