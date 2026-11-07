package com.repp.aquaticremineztools.presentation.manga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.manga.model.MangaChapter
import com.repp.aquaticremineztools.data.manga.model.MangaSeries
import com.repp.aquaticremineztools.data.manga.repository.MangaRepository
import kotlinx.coroutines.launch

sealed class AnalyzeState {
    object Idle : AnalyzeState()
    object Loading : AnalyzeState()
    data class Success(val series: MangaSeries) : AnalyzeState()
    data class Error(val message: String) : AnalyzeState()
}

class MangaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MangaRepository(application)

    private val _analyzeState = MutableLiveData<AnalyzeState>(AnalyzeState.Idle)
    val analyzeState: LiveData<AnalyzeState> = _analyzeState

    val activeDownloads: LiveData<List<MangaDownloadEntity>> = repository.getActiveAndQueuedLiveData()
    val activeAndQueuedCount: LiveData<Int> = repository.getActiveAndQueuedCountLiveData()

    private var currentSeries: MangaSeries? = null

    fun analyzeUrl(url: String) {
        if (url.isBlank()) {
            _analyzeState.value = AnalyzeState.Error("URL cannot be empty.")
            return
        }

        _analyzeState.value = AnalyzeState.Loading
        viewModelScope.launch {
            try {
                val series = repository.analyzeUrl(url)
                currentSeries = series
                _analyzeState.value = AnalyzeState.Success(series)
            } catch (e: Exception) {
                _analyzeState.value = AnalyzeState.Error(e.localizedMessage ?: "Failed to parse URL.")
            }
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
        viewModelScope.launch {
            val count = repository.enqueueAllChapters(series.title, series.chapters)
            onResult(count)
        }
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            repository.cancelDownload(downloadId)
        }
    }
}
