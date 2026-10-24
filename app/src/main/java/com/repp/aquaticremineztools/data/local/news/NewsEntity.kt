package com.repp.aquaticremineztools.data.local.news

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.repp.aquaticremineztools.data.model.NewsArticle

@Entity(tableName = "news_articles")
data class NewsEntity(
    @PrimaryKey
    val articleUrl: String,
    val headline: String,
    val summary: String,
    val content: String?,
    val thumbnailUrl: String?,
    val publisher: String,
    val category: String,
    val publishedAtEpoch: Long,
    val language: String,
    val fetchedAtEpoch: Long = System.currentTimeMillis()
) {
    fun toArticle(isFeatured: Boolean = false): NewsArticle {
        return NewsArticle(
            id = articleUrl,
            headline = headline,
            summary = summary,
            content = content,
            thumbnailUrl = thumbnailUrl,
            articleUrl = articleUrl,
            publisher = publisher,
            category = category,
            publishedAtEpoch = publishedAtEpoch,
            language = language,
            isFeatured = isFeatured
        )
    }

    companion object {
        fun fromArticle(article: NewsArticle): NewsEntity {
            return NewsEntity(
                articleUrl = article.articleUrl,
                headline = article.headline,
                summary = article.summary,
                content = article.content,
                thumbnailUrl = article.thumbnailUrl,
                publisher = article.publisher,
                category = article.category,
                publishedAtEpoch = article.publishedAtEpoch,
                language = article.language
            )
        }
    }
}
