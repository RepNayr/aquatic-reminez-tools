package com.repp.aquaticremineztools.data.repository

import android.content.Context
import android.util.Log
import com.repp.aquaticremineztools.core.settings.NewsPreferences
import com.repp.aquaticremineztools.data.local.news.NewsDatabase
import com.repp.aquaticremineztools.data.local.news.NewsEntity
import com.repp.aquaticremineztools.data.model.NewsArticle
import com.repp.aquaticremineztools.data.remote.news.RssFeedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.Locale

data class NewsFetchResult(
    val articles: List<NewsArticle>,
    val isCached: Boolean,
    val isOffline: Boolean,
    val lastUpdatedEpoch: Long,
    val errorMessage: String? = null
)

class NewsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val newsDb = NewsDatabase.getInstance(appContext)
    private val newsDao = newsDb.newsDao()
    private val rssFeedService = RssFeedService()

    companion object {
        private const val TAG = "NewsRepository"
        private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
        private const val REFRESH_THROTTLE_MS = 30 * 60 * 1000L // 30 mins

        @Volatile
        private var INSTANCE: NewsRepository? = null

        fun getInstance(context: Context): NewsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NewsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun getHomepageArticles(forceRefresh: Boolean = false): Result<NewsFetchResult> = withContext(Dispatchers.IO) {
        val fullResult = getArticles(category = null, searchQuery = null, forceRefresh = forceRefresh)
        if (fullResult.isFailure) return@withContext fullResult

        val fetchResult = fullResult.getOrNull() ?: return@withContext Result.failure(Exception("No data available"))
        val allArticles = fetchResult.articles

        if (allArticles.isEmpty()) {
            return@withContext Result.success(fetchResult.copy(articles = emptyList()))
        }

        // Pick most recent article with thumbnail as Featured article
        val featuredCandidate = allArticles.firstOrNull { !it.thumbnailUrl.isNullOrBlank() } ?: allArticles.first()
        val featuredArticle = featuredCandidate.copy(isFeatured = true)

        // Remaining 7 compact articles
        val remaining = allArticles.filter { it.articleUrl != featuredArticle.articleUrl }.take(7)

        val homepageArticles = mutableListOf<NewsArticle>()
        homepageArticles.add(featuredArticle)
        homepageArticles.addAll(remaining)

        Result.success(fetchResult.copy(articles = homepageArticles))
    }

    suspend fun getArticles(
        category: String? = null,
        searchQuery: String? = null,
        forceRefresh: Boolean = false
    ): Result<NewsFetchResult> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val lastFetch = NewsPreferences.getLastFetchTimestamp(appContext)
        val languagePref = NewsPreferences.getNewsLanguage(appContext)

        // Cleanup expired articles older than 7 days
        val cutoff = currentTime - SEVEN_DAYS_MS
        newsDao.deleteOlderThan(cutoff)

        val shouldFetchRemote = forceRefresh || (currentTime - lastFetch > REFRESH_THROTTLE_MS) || newsDao.getCount() == 0

        var fetchError: String? = null
        var isOffline = false

        if (shouldFetchRemote) {
            try {
                val matchingSources = RssFeedService.RSS_SOURCES.filter { config ->
                    (languagePref == "all" || config.language == languagePref) &&
                            (category == null || category.equals("All", ignoreCase = true) || config.category.equals(category, ignoreCase = true))
                }

                val sourceListToFetch = if (matchingSources.isNotEmpty()) matchingSources else RssFeedService.RSS_SOURCES

                val deferreds = sourceListToFetch.map { config ->
                    async { rssFeedService.fetchFeed(config) }
                }

                val remoteResults = deferreds.awaitAll().flatten()

                if (remoteResults.isNotEmpty()) {
                    val deduplicated = deduplicateArticles(remoteResults)
                    val entities = deduplicated.map { NewsEntity.fromArticle(it) }

                    newsDao.insertArticles(entities)
                    NewsPreferences.setLastFetchTimestamp(appContext, currentTime)
                } else {
                    isOffline = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote news: ${e.message}")
                fetchError = e.localizedMessage
                isOffline = true
            }
        }

        // Query local cache
        val cachedEntities = when {
            !searchQuery.isNullOrBlank() -> newsDao.searchArticles(searchQuery)
            category != null && !category.equals("All", ignoreCase = true) -> newsDao.getArticlesByCategory(category)
            else -> newsDao.getAllArticles()
        }

        val filteredByLang = cachedEntities.map { it.toArticle() }.filter { article ->
            languagePref == "all" || article.language == languagePref
        }

        val sortedArticles = filteredByLang.sortedByDescending { it.publishedAtEpoch }

        Result.success(
            NewsFetchResult(
                articles = sortedArticles,
                isCached = !shouldFetchRemote || isOffline,
                isOffline = isOffline,
                lastUpdatedEpoch = NewsPreferences.getLastFetchTimestamp(appContext),
                errorMessage = fetchError
            )
        )
    }

    private fun deduplicateArticles(articles: List<NewsArticle>): List<NewsArticle> {
        val seenUrls = mutableSetOf<String>()
        val result = mutableListOf<NewsArticle>()

        for (article in articles) {
            val urlKey = article.articleUrl.trim().lowercase(Locale.ROOT)
            if (urlKey in seenUrls) continue

            seenUrls.add(urlKey)
            result.add(article)
        }
        return result
    }
}
