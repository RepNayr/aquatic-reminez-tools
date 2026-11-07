package com.repp.aquaticremineztools.data.manga.model

data class MangaSeries(
    val title: String,
    val coverUrl: String,
    val description: String? = null,
    val seriesUrl: String,
    val chapters: List<MangaChapter>
)

data class MangaChapter(
    val title: String,
    val chapterUrl: String,
    val pageCount: Int = -1,
    val orderIndex: Int
)

data class MangaPage(
    val pageIndex: Int,
    val imageUrl: String
)
