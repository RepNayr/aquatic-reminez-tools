package com.repp.aquaticremineztools.data.manga.model

enum class ChapterAnalysisStatus {
    PENDING,
    ANALYZING,
    SUCCESS,
    FAILED
}

data class MangaSeries(
    val title: String,
    val coverUrl: String,
    val description: String? = null,
    val seriesUrl: String,
    val chapters: List<MangaChapter>
)

data class MangaChapter(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val chapterUrl: String,
    val pageCount: Int = -1,
    val orderIndex: Int,
    val status: ChapterAnalysisStatus = ChapterAnalysisStatus.PENDING,
    val pages: List<String> = emptyList(),
    val errorMessage: String? = null
)

data class MangaPage(
    val pageIndex: Int,
    val imageUrl: String
)
