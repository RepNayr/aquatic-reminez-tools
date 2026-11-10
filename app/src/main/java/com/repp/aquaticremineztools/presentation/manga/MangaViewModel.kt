package com.repp.aquaticremineztools.presentation.manga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.local.manga.MangaResultEntity
import com.repp.aquaticremineztools.data.manga.model.ChapterAnalysisStatus
import com.repp.aquaticremineztools.data.manga.model.MangaChapter
import com.repp.aquaticremineztools.data.manga.model.MangaSeries
import com.repp.aquaticremineztools.data.manga.repository.MangaRepository
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.data.manga.source.MangaAuthRequiredException
import com.repp.aquaticremineztools.data.manga.source.MangaParseException
import com.repp.aquaticremineztools.data.manga.source.SessionValidationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AnalyzeState {
    object Idle : AnalyzeState()
    data class Analyzing(
        val seriesTitle: String,
        val coverUrl: String,
        val currentChapterIndex: Int,
        val totalChapters: Int,
        val progressText: String,
        val series: MangaSeries
    ) : AnalyzeState()
    data class Success(val series: MangaSeries) : AnalyzeState()
    data class Error(
        val message: String,
        val isInvalidUrl: Boolean = false,
        val isNoChaptersFound: Boolean = false
    ) : AnalyzeState()
    data class ShowReanalysisConfirmation(val url: String) : AnalyzeState()
}

sealed class SessionState {
    object NotLoggedIn : SessionState()
    object Checking : SessionState()
    data class LoggedIn(val timestampFormatted: String) : SessionState()
    object ConnectionError : SessionState()
}

class MangaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MangaRepository(application)

    private val _analyzeState = MutableLiveData<AnalyzeState>(AnalyzeState.Idle)
    val analyzeState: LiveData<AnalyzeState> = _analyzeState

    private val _sessionState = MutableLiveData<SessionState>(SessionState.NotLoggedIn)
    val sessionState: LiveData<SessionState> = _sessionState

    val activeDownloads: LiveData<List<MangaDownloadEntity>> = repository.getActiveAndQueuedLiveData()
    val completedDownloads: LiveData<List<MangaDownloadEntity>> = repository.getCompletedDownloadsLiveData()
    val mangaResults: LiveData<List<MangaResultEntity>> = repository.getResultsLiveData()
    val activeAndQueuedCount: LiveData<Int> = repository.getActiveAndQueuedCountLiveData()

    var currentSeries: MangaSeries? = null
        private set

    private var lastAnalyzedUrl: String? = null
    private var analysisJob: Job? = null

    init {
        checkSessionState()
        clearResultsOnStart()
    }

    fun clearResultsOnStart() {
        viewModelScope.launch {
            repository.clearAllResults()
        }
    }

    fun checkSessionState() {
        if (!CrotPediaSessionStore.isLoggedIn(getApplication())) {
            _sessionState.value = SessionState.NotLoggedIn
            return
        }

        _sessionState.value = SessionState.Checking
        viewModelScope.launch {
            when (val result = repository.validateSessionResult()) {
                is SessionValidationResult.Valid -> {
                    val timeStr = formatTimestamp(CrotPediaSessionStore.getLastLoginTime(getApplication()))
                    _sessionState.value = SessionState.LoggedIn(timeStr)
                }
                is SessionValidationResult.Unauthenticated -> {
                    repository.logout()
                    _sessionState.value = SessionState.NotLoggedIn
                }
                is SessionValidationResult.AccessDenied -> {
                    val timeStr = formatTimestamp(CrotPediaSessionStore.getLastLoginTime(getApplication()))
                    _sessionState.value = SessionState.LoggedIn("$timeStr (Access Denied)")
                }
                is SessionValidationResult.ChallengeRequired -> {
                    val timeStr = formatTimestamp(CrotPediaSessionStore.getLastLoginTime(getApplication()))
                    _sessionState.value = SessionState.LoggedIn("$timeStr (Challenge)")
                }
                is SessionValidationResult.NetworkError -> {
                    _sessionState.value = SessionState.ConnectionError
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        _sessionState.value = SessionState.NotLoggedIn
    }

    private fun formatTimestamp(timeMs: Long): String {
        if (timeMs <= 0L) return ""
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            sdf.format(Date(timeMs))
        } catch (_: Exception) {
            ""
        }
    }

    fun analyzeUrl(url: String, forceReanalysis: Boolean = false) {
        val cleanInput = url.trim()
        if (cleanInput.isEmpty() || !CrotPediaParser.isAllowedUrl(cleanInput)) {
            _analyzeState.value = AnalyzeState.Error(
                "URL harus berasal dari domain crotpedia.net yang sah.",
                isInvalidUrl = true
            )
            return
        }

        val canonical = canonicalUrl(cleanInput)
        if (!forceReanalysis && lastAnalyzedUrl != null && canonical == lastAnalyzedUrl && currentSeries != null) {
            _analyzeState.value = AnalyzeState.ShowReanalysisConfirmation(url)
            return
        }

        cancelAnalysis()

        analysisJob = viewModelScope.launch {
            _analyzeState.value = AnalyzeState.Analyzing(
                seriesTitle = "Mengambil data series...",
                coverUrl = "",
                currentChapterIndex = 0,
                totalChapters = 0,
                progressText = "Menghubungkan ke server...",
                series = MangaSeries("", "", null, cleanInput, emptyList())
            )

            try {
                val initialSeries = repository.parseSeries(cleanInput)
                if (initialSeries.chapters.isEmpty()) {
                    _analyzeState.value = AnalyzeState.Error("No Chapters Found", isNoChaptersFound = true)
                    return@launch
                }

                lastAnalyzedUrl = canonical
                val chapters = initialSeries.chapters.map { it.copy(status = ChapterAnalysisStatus.PENDING) }.toMutableList()
                var workingSeries = initialSeries.copy(chapters = chapters.toList())
                currentSeries = workingSeries

                val total = chapters.size
                for (i in 0 until total) {
                    coroutineContext.ensureActive()
                    val ch = chapters[i]

                    chapters[i] = ch.copy(status = ChapterAnalysisStatus.ANALYZING)
                    workingSeries = workingSeries.copy(chapters = chapters.toList())
                    currentSeries = workingSeries

                    val progressMsg = "Analyzing Chapter ${i + 1}/$total"
                    _analyzeState.value = AnalyzeState.Analyzing(
                        seriesTitle = workingSeries.title,
                        coverUrl = workingSeries.coverUrl,
                        currentChapterIndex = i + 1,
                        totalChapters = total,
                        progressText = progressMsg,
                        series = workingSeries
                    )

                    try {
                        val pages = repository.parsePagesForChapter(ch.chapterUrl)
                        if (pages.isEmpty()) {
                            chapters[i] = ch.copy(
                                status = ChapterAnalysisStatus.FAILED,
                                errorMessage = "No valid images found"
                            )
                        } else {
                            chapters[i] = ch.copy(
                                status = ChapterAnalysisStatus.SUCCESS,
                                pageCount = pages.size,
                                pages = pages.map { it.imageUrl },
                                errorMessage = null
                            )
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        if (e is MangaAuthRequiredException) {
                            repository.logout()
                            _sessionState.value = SessionState.NotLoggedIn
                        }
                        chapters[i] = ch.copy(
                            status = ChapterAnalysisStatus.FAILED,
                            errorMessage = toUserErrorMessage(e)
                        )
                    }

                    workingSeries = workingSeries.copy(chapters = chapters.toList())
                    currentSeries = workingSeries

                    _analyzeState.value = AnalyzeState.Analyzing(
                        seriesTitle = workingSeries.title,
                        coverUrl = workingSeries.coverUrl,
                        currentChapterIndex = i + 1,
                        totalChapters = total,
                        progressText = progressMsg,
                        series = workingSeries
                    )

                    delay(100)
                }

                if (isActive) {
                    _analyzeState.value = AnalyzeState.Success(workingSeries)
                }
            } catch (e: CancellationException) {
                currentSeries = null
                _analyzeState.value = AnalyzeState.Idle
                throw e
            } catch (e: Exception) {
                currentSeries = null
                if (e is MangaAuthRequiredException) {
                    repository.logout()
                    _sessionState.value = SessionState.NotLoggedIn
                    _analyzeState.value = AnalyzeState.Error("SESSION EXPIRED. Silakan login kembali.")
                } else {
                    _analyzeState.value = AnalyzeState.Error(e.message ?: "Analysis failed")
                }
            }
        }
    }

    fun retryChapter(chapterId: String) {
        val series = currentSeries ?: return
        val index = series.chapters.indexOfFirst { it.id == chapterId || it.chapterUrl == chapterId }
        if (index == -1) return

        val ch = series.chapters[index]
        if (ch.status == ChapterAnalysisStatus.ANALYZING) return

        viewModelScope.launch {
            val chapters = series.chapters.toMutableList()
            chapters[index] = ch.copy(status = ChapterAnalysisStatus.ANALYZING, errorMessage = null)
            val updatedSeries = series.copy(chapters = chapters.toList())
            currentSeries = updatedSeries
            _analyzeState.value = AnalyzeState.Success(updatedSeries)

            try {
                val pages = repository.parsePagesForChapter(ch.chapterUrl)
                if (pages.isEmpty()) {
                    chapters[index] = ch.copy(
                        status = ChapterAnalysisStatus.FAILED,
                        errorMessage = "No valid images found"
                    )
                } else {
                    chapters[index] = ch.copy(
                        status = ChapterAnalysisStatus.SUCCESS,
                        pageCount = pages.size,
                        pages = pages.map { it.imageUrl },
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e is MangaAuthRequiredException) {
                    repository.logout()
                    _sessionState.value = SessionState.NotLoggedIn
                }
                chapters[index] = ch.copy(
                    status = ChapterAnalysisStatus.FAILED,
                    errorMessage = toUserErrorMessage(e)
                )
            }

            val finalSeries = series.copy(chapters = chapters.toList())
            currentSeries = finalSeries
            _analyzeState.value = AnalyzeState.Success(finalSeries)
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        currentSeries = null
        _analyzeState.value = AnalyzeState.Idle
    }

    fun onCancelReanalysis() {
        val series = currentSeries
        if (series != null) {
            _analyzeState.value = AnalyzeState.Success(series)
        } else {
            _analyzeState.value = AnalyzeState.Idle
        }
    }

    fun downloadChapter(chapter: MangaChapter, onResult: (Boolean) -> Unit) {
        val seriesTitle = currentSeries?.title ?: "Manga"
        viewModelScope.launch {
            val enqueued = repository.enqueueChapter(seriesTitle, chapter)
            onResult(enqueued)
        }
    }

    fun downloadAllChapters(onResult: (Int) -> Unit) {
        val series = currentSeries ?: return
        val successfulChapters = series.chapters.filter { it.status == ChapterAnalysisStatus.SUCCESS }
        viewModelScope.launch {
            val count = repository.enqueueAllChapters(series.title, successfulChapters)
            onResult(count)
        }
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            repository.cancelDownload(downloadId)
        }
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            repository.deleteDownload(downloadId)
        }
    }

    fun retryDownload(entity: MangaDownloadEntity) {
        viewModelScope.launch {
            repository.retryDownload(entity)
        }
    }

    fun deleteResult(resultId: Long) {
        viewModelScope.launch {
            repository.deleteResult(resultId)
        }
    }

    fun compressResult(result: MangaResultEntity, targetSizeBytes: Long) {
        viewModelScope.launch {
            repository.compressResult(result, targetSizeBytes)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelAnalysis()
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

    private fun toUserErrorMessage(e: Throwable): String {
        val message = e.message ?: ""
        val lower = message.lowercase()
        return when {
            e is java.net.UnknownHostException -> "Network connection failed"
            e is java.net.SocketTimeoutException -> "Server timeout"
            e is android.os.NetworkOnMainThreadException -> "Network operation error"
            e is MangaAuthRequiredException -> {
                when {
                    lower.contains("401") -> "Access denied (401)"
                    lower.contains("403") -> "Access denied (403)"
                    else -> "Session expired / Login required"
                }
            }
            e is MangaParseException -> {
                when {
                    lower.contains("404") -> "Chapter not found (404)"
                    lower.contains("429") -> "Rate limited (HTTP 429)"
                    lower.contains("500") || lower.contains("502") || lower.contains("503") || lower.contains("server error") -> "Server error (5xx)"
                    lower.contains("kosong") || lower.contains("empty") -> "Empty response"
                    lower.contains("container") -> "Reader not detected"
                    lower.contains("gambar") || lower.contains("image") || lower.contains("valid") -> "No valid images found"
                    else -> message.ifBlank { "Analysis failed" }
                }
            }
            lower.contains("404") -> "Chapter not found (404)"
            lower.contains("403") -> "Access denied (403)"
            lower.contains("429") -> "Rate limited (HTTP 429)"
            lower.contains("timeout") -> "Server timeout"
            lower.contains("unknownhost") || lower.contains("unable to resolve") -> "Network connection failed"
            else -> if (message.isNotBlank() && !message.contains("Exception")) message else "Analysis failed"
        }
    }
}
