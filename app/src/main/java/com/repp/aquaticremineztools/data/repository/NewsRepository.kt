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

        // Remaining 4 compact articles (Total 5 articles on homepage: 1 featured + 4 compact)
        val remaining = allArticles.filter { it.articleUrl != featuredArticle.articleUrl }.take(4)

        val homepageArticles = mutableListOf<NewsArticle>()
        homepageArticles.add(featuredArticle)
        homepageArticles.addAll(remaining)

        // Pre-fetch full content for these 5 homepage articles so it's ready instantly on click
        homepageArticles.forEach { article ->
            if (article.content.isNullOrBlank() || article.content.length < 300) {
                try {
                    val full = fetchFullArticleContent(article.articleUrl)
                    if (full.isNotBlank()) {
                        newsDao.updateArticleContent(article.articleUrl, full)
                    }
                } catch (_: Exception) {}
            }
        }

        val updatedHomepageArticles = homepageArticles.map { art ->
            val entity = newsDao.getAllArticles().find { it.articleUrl == art.articleUrl }
            if (entity != null && !entity.content.isNullOrBlank()) {
                art.copy(content = entity.content)
            } else {
                art
            }
        }

        Result.success(fetchResult.copy(articles = updatedHomepageArticles))
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

    suspend fun fetchFullArticleContent(articleUrl: String): String = withContext(Dispatchers.IO) {
        if (articleUrl.isBlank()) return@withContext ""
        try {
            val doc = org.jsoup.Jsoup.connect(articleUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()

            // Remove non-content elements
            doc.select("script, style, nav, header, footer, iframe, .ad, .ads, .advertisement, .comment, .related, .share, .tags, .author-bio").remove()

            // Select candidate container for main content
            val bodyElement = doc.select("article, [itemprop=articleBody], .detail-text, .detail__body-text, .post-content, .entry-content, .read__content, .content-detail, .text-detail, .post-body, .article-content, .caas-body").firstOrNull()
                ?: doc.body()

            val paragraphs = bodyElement.select("p")
                .map { it.text().trim() }
                .filter { p ->
                    p.length > 20 &&
                    !p.startsWith("BACA JUGA", ignoreCase = true) &&
                    !p.startsWith("Simak Video", ignoreCase = true) &&
                    !p.contains("Copyright", ignoreCase = true) &&
                    !p.contains("All rights reserved", ignoreCase = true)
                }

            if (paragraphs.isNotEmpty()) {
                val fullText = paragraphs.joinToString("\n\n")
                newsDao.updateArticleContent(articleUrl, fullText)
                return@withContext fullText
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scrape full article content from $articleUrl: ${e.message}")
        }
        return@withContext ""
    }
}
