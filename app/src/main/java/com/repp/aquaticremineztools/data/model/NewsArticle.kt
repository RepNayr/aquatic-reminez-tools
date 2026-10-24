package com.repp.aquaticremineztools.data.model

data class NewsArticle(
    val id: String,
    val headline: String,
    val summary: String,
    val content: String? = null,
    val thumbnailUrl: String? = null,
    val articleUrl: String,
    val publisher: String,
    val category: String,
    val publishedAtEpoch: Long,
    val language: String,
    val isFeatured: Boolean = false
)
