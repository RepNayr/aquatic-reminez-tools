package com.repp.aquaticremineztools.data.local.news

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NewsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsEntity>)

    @Query("SELECT * FROM news_articles ORDER BY publishedAtEpoch DESC")
    suspend fun getAllArticles(): List<NewsEntity>

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY publishedAtEpoch DESC")
    suspend fun getArticlesByCategory(category: String): List<NewsEntity>

    @Query("SELECT * FROM news_articles WHERE headline LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR publisher LIKE '%' || :query || '%' ORDER BY publishedAtEpoch DESC")
    suspend fun searchArticles(query: String): List<NewsEntity>

    @Query("DELETE FROM news_articles WHERE publishedAtEpoch < :cutoffEpoch")
    suspend fun deleteOlderThan(cutoffEpoch: Long)

    @Query("DELETE FROM news_articles")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getCount(): Int
}
