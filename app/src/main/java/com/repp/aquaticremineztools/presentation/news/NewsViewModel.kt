package com.repp.aquaticremineztools.presentation.news

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.model.NewsArticle
import com.repp.aquaticremineztools.data.repository.NewsRepository
import kotlinx.coroutines.launch

sealed class NewsUiState {
    object Loading : NewsUiState()
    data class Success(
        val articles: List<NewsArticle>,
        val isCached: Boolean,
        val isOffline: Boolean,
        val lastUpdatedEpoch: Long,
        val hasMore: Boolean = false
    ) : NewsUiState()
    data class Error(
        val message: String
    ) : NewsUiState()
}

class NewsViewModel : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _homepageUiState = MutableLiveData<NewsUiState>(NewsUiState.Loading)
    val homepageUiState: LiveData<NewsUiState> = _homepageUiState

    private val _newsFeedUiState = MutableLiveData<NewsUiState>(NewsUiState.Loading)
    val newsFeedUiState: LiveData<NewsUiState> = _newsFeedUiState

    private var allArticlesCache: List<NewsArticle> = emptyList()
    private var currentPage: Int = 1

    fun loadHomepageNews(context: Context, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh || _homepageUiState.value !is NewsUiState.Success) {
                _homepageUiState.value = NewsUiState.Loading
            }

            val repository = NewsRepository.getInstance(context)
            val result = repository.getHomepageArticles(forceRefresh)

            result.onSuccess { fetchResult ->
                _homepageUiState.value = NewsUiState.Success(
                    articles = fetchResult.articles,
                    isCached = fetchResult.isCached,
                    isOffline = fetchResult.isOffline,
                    lastUpdatedEpoch = fetchResult.lastUpdatedEpoch
                )
            }.onFailure { exception ->
                _homepageUiState.value = NewsUiState.Error(
                    message = exception.localizedMessage ?: "Failed to load news"
                )
            }
        }
    }

    fun loadNewsFeed(
        context: Context,
        category: String? = null,
        searchQuery: String? = null,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            if (forceRefresh || _newsFeedUiState.value !is NewsUiState.Success) {
                _newsFeedUiState.value = NewsUiState.Loading
            }

            currentPage = 1
            val repository = NewsRepository.getInstance(context)
            val result = repository.getArticles(category, searchQuery, forceRefresh)

            result.onSuccess { fetchResult ->
                allArticlesCache = fetchResult.articles
                val pagedArticles = allArticlesCache.take(PAGE_SIZE)
                val hasMore = allArticlesCache.size > pagedArticles.size

                _newsFeedUiState.value = NewsUiState.Success(
                    articles = pagedArticles,
                    isCached = fetchResult.isCached,
                    isOffline = fetchResult.isOffline,
                    lastUpdatedEpoch = fetchResult.lastUpdatedEpoch,
                    hasMore = hasMore
                )
            }.onFailure { exception ->
                _newsFeedUiState.value = NewsUiState.Error(
                    message = exception.localizedMessage ?: "Failed to load news feed"
                )
            }
        }
    }

    fun loadMoreNews() {
        val currentState = _newsFeedUiState.value
        if (currentState is NewsUiState.Success) {
            currentPage++
            val nextSize = currentPage * PAGE_SIZE
            val pagedArticles = allArticlesCache.take(nextSize)
            val hasMore = allArticlesCache.size > pagedArticles.size

            _newsFeedUiState.value = currentState.copy(
                articles = pagedArticles,
                hasMore = hasMore
            )
        }
    }
}
