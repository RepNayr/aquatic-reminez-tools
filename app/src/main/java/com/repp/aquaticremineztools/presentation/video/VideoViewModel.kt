package com.repp.aquaticremineztools.presentation.video

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.local.video.VideoDownloadEntity
import com.repp.aquaticremineztools.data.local.video.VideoResultEntity
import com.repp.aquaticremineztools.data.video.model.VideoEpisode
import com.repp.aquaticremineztools.data.video.model.VideoSeries
import com.repp.aquaticremineztools.data.video.repository.VideoRepository
import com.repp.aquaticremineztools.data.video.source.HentaiCopParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class AnalyzeVideoState {
    object Idle : AnalyzeVideoState()
    data class Analyzing(val seriesUrl: String) : AnalyzeVideoState()
    data class Success(val series: VideoSeries) : AnalyzeVideoState()
    data class Error(val message: String) : AnalyzeVideoState()
}

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val parser = HentaiCopParser(application)

    val activeDownloads: LiveData<List<VideoDownloadEntity>> = repository.activeDownloads
    val videoResults: LiveData<List<VideoResultEntity>> = repository.videoResults

    val activeAndQueuedCount: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(activeDownloads) { list ->
            value = list?.count {
                it.status == VideoDownloadEntity.STATUS_DOWNLOADING ||
                        it.status == VideoDownloadEntity.STATUS_QUEUED
            } ?: 0
        }
    }

    private val _analyzeState = MutableLiveData<AnalyzeVideoState>(AnalyzeVideoState.Idle)
    val analyzeState: LiveData<AnalyzeVideoState> = _analyzeState

    private var currentSeries: VideoSeries? = null
    private var analyzeJob: Job? = null

    // Saved Configurations: Episode ID -> Pair<ServerId, QualityLabel>
    private val savedConfigs = mutableMapOf<String, Pair<String, String>>()

    fun analyzeUrl(url: String) {
        val canonical = HentaiCopParser.canonicalizeUrl(url)
        if (!HentaiCopParser.isAllowedSeriesUrl(canonical)) {
            _analyzeState.value = AnalyzeVideoState.Error("URL tidak valid. Harap masukkan URL series HentaiCop yang valid (misal: https://hentaicop.com/series/hitori-tsuma/).")
            return
        }

        analyzeJob?.cancel()
        _analyzeState.value = AnalyzeVideoState.Analyzing(canonical)

        analyzeJob = viewModelScope.launch {
            try {
                val series = parser.parseSeries(canonical)

                // For each episode, fetch details to discover available servers/qualities
                val fullEpisodes = series.episodes.map { ep ->
                    try {
                        val details = parser.parseEpisodeDetails(ep.url)
                        details.copy(
                            title = ep.title,
                            order = ep.order
                        )
                    } catch (_: Exception) {
                        ep
                    }
                }

                val updatedSeries = series.copy(episodes = fullEpisodes)
                currentSeries = updatedSeries

                // Auto-configure 720p for episodes that offer it
                fullEpisodes.forEach { ep ->
                    val server720p = ep.availableSources.find { it.quality.height == 720 }
                    if (server720p != null) {
                        savedConfigs[ep.id] = Pair(server720p.serverId, server720p.quality.label)
                    }
                }

                _analyzeState.value = AnalyzeVideoState.Success(buildConfiguredSeries(updatedSeries))

                // Pre-resolve direct media sources in background for instant Bottom Sheet loading
                fullEpisodes.forEach { ep ->
                    ep.availableSources.forEach { src ->
                        viewModelScope.launch {
                            try {
                                val resolved = parser.resolveDirectMediaSource(src)
                                preResolvedSources[src.downloadPageUrl] = resolved
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                _analyzeState.value = AnalyzeVideoState.Error(e.localizedMessage ?: "Gagal menganalisis series.")
            }
        }
    }

    private val preResolvedSources = java.util.concurrent.ConcurrentHashMap<String, com.repp.aquaticremineztools.data.video.model.ResolvedVideoSource>()

    fun getPreResolvedSource(downloadPageUrl: String): com.repp.aquaticremineztools.data.video.model.ResolvedVideoSource? {
        return preResolvedSources[downloadPageUrl]
    }

    fun saveEpisodeConfig(episodeId: String, serverId: String, qualityLabel: String) {
        savedConfigs[episodeId] = Pair(serverId, qualityLabel)
        currentSeries?.let { series ->
            _analyzeState.value = AnalyzeVideoState.Success(buildConfiguredSeries(series))
        }
    }

    fun getSavedConfig(episodeId: String): Pair<String, String>? {
        return savedConfigs[episodeId]
    }

    fun getUnconfiguredEpisodes(): List<VideoEpisode> {
        val series = currentSeries ?: return emptyList()
        return series.episodes.filter { ep ->
            val hasSaved = savedConfigs.containsKey(ep.id)
            val hasDefault720 = ep.availableSources.any { it.quality.height == 720 }
            !hasSaved && !hasDefault720
        }
    }

    fun downloadEpisode(episode: VideoEpisode, onResult: (Boolean) -> Unit) {
        val series = currentSeries ?: return
        val config = savedConfigs[episode.id]
        val serverId = config?.first ?: episode.availableSources.find { it.quality.height == 720 }?.serverId ?: ""
        val qualityLabel = config?.second ?: episode.availableSources.find { it.quality.height == 720 }?.quality?.label ?: "720p"

        viewModelScope.launch {
            val enqueued = repository.enqueueDownload(series.title, episode, serverId, qualityLabel)
            onResult(enqueued)
        }
    }

    fun downloadAllEpisodes(onResult: (addedCount: Int) -> Unit) {
        val series = currentSeries ?: return
        var enqueuedCount = 0

        viewModelScope.launch {
            series.episodes.forEach { ep ->
                val config = savedConfigs[ep.id]
                val serverId = config?.first ?: ep.availableSources.find { it.quality.height == 720 }?.serverId ?: ""
                val qualityLabel = config?.second ?: ep.availableSources.find { it.quality.height == 720 }?.quality?.label ?: "720p"

                val success = repository.enqueueDownload(series.title, ep, serverId, qualityLabel)
                if (success) enqueuedCount++
            }
            onResult(enqueuedCount)
        }
    }

    fun compressResult(result: VideoResultEntity, targetSizeBytes: Long) {
        viewModelScope.launch {
            repository.enqueueCompression(result, targetSizeBytes)
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            repository.cancelDownload(id)
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun deleteResult(id: Long) {
        viewModelScope.launch {
            repository.deleteResult(id)
        }
    }

    private fun buildConfiguredSeries(series: VideoSeries): VideoSeries {
        val configuredEpisodes = series.episodes.map { ep ->
            val config = savedConfigs[ep.id]
            if (config != null) {
                ep.copy(
                    savedServerId = config.first,
                    savedQualityLabel = config.second
                )
            } else {
                val default720 = ep.availableSources.find { it.quality.height == 720 }
                if (default720 != null) {
                    ep.copy(
                        savedServerId = default720.serverId,
                        savedQualityLabel = default720.quality.label
                    )
                } else {
                    ep
                }
            }
        }
        return series.copy(episodes = configuredEpisodes)
    }

    fun cancelAnalysis() {
        analyzeJob?.cancel()
        _analyzeState.value = AnalyzeVideoState.Idle
    }
}
