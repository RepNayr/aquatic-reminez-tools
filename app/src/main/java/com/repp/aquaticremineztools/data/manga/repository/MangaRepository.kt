package com.repp.aquaticremineztools.data.manga.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.repp.aquaticremineztools.data.local.manga.MangaDatabase
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.manga.model.MangaChapter
import com.repp.aquaticremineztools.data.manga.model.MangaSeries
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.domain.manga.worker.MangaDownloadWorker

class MangaRepository(context: Context) {

    private val db = MangaDatabase.getInstance(context)
    private val dao = db.mangaDownloadDao()
    private val parser = CrotPediaParser()
    private val appContext = context.applicationContext

    suspend fun analyzeUrl(url: String): MangaSeries {
        return parser.parseUrl(url)
    }

    suspend fun enqueueChapter(seriesTitle: String, chapter: MangaChapter): Boolean {
        val cleanUrl = chapter.chapterUrl.trim()
        val normalizedUrl = if (cleanUrl.endsWith("/")) cleanUrl.dropLast(1) else "$cleanUrl/"

        val existingCount = dao.countExistingChapter(cleanUrl, normalizedUrl)
        if (existingCount > 0) {
            return false // Deduplication: already queued, downloading or completed
        }

        val sanitizeTitle = sanitizeFileName("${seriesTitle}_${chapter.title}.pdf")
        val entity = MangaDownloadEntity(
            seriesTitle = seriesTitle,
            chapterTitle = chapter.title,
            chapterUrl = cleanUrl,
            outputFileName = sanitizeTitle,
            totalPages = chapter.pageCount,
            status = MangaDownloadEntity.STATUS_QUEUED
        )

        dao.insert(entity)
        MangaDownloadWorker.enqueueWork(appContext)
        return true
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

    fun getActiveAndQueuedLiveData(): LiveData<List<MangaDownloadEntity>> {
        return dao.getActiveAndQueuedLiveData()
    }

    fun getCompletedDownloadsLiveData(): LiveData<List<MangaDownloadEntity>> {
        return dao.getCompletedDownloadsLiveData()
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
