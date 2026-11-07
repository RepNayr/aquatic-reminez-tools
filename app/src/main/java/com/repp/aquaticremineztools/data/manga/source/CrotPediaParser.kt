package com.repp.aquaticremineztools.data.manga.source

import com.repp.aquaticremineztools.data.manga.model.MangaChapter
import com.repp.aquaticremineztools.data.manga.model.MangaPage
import com.repp.aquaticremineztools.data.manga.model.MangaSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.concurrent.TimeUnit

class CrotPediaParser(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        fun isAllowedUrl(url: String): Boolean {
            return try {
                val uri = URI(url.trim())
                val host = uri.host?.lowercase() ?: return false
                host == "crotpedia.net" || host.endsWith(".crotpedia.net") || host == "www.crotpedia.net"
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun parseUrl(inputUrl: String): MangaSeries = withContext(Dispatchers.IO) {
        val cleanUrl = inputUrl.trim()
        if (!isAllowedUrl(cleanUrl)) {
            throw IllegalArgumentException("URL must belong to crotpedia.net domain.")
        }

        val doc = fetchDocument(cleanUrl)

        if (cleanUrl.contains("/series/")) {
            parseSeriesDoc(cleanUrl, doc)
        } else {
            parseSingleChapterAsSeries(cleanUrl, doc)
        }
    }

    private fun fetchDocument(url: String): Document {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://crotpedia.net/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to load page: HTTP ${response.code}")
            }
            val html = response.body?.string()
                ?: throw IllegalStateException("Empty response body from server.")
            return Jsoup.parse(html, url)
        }
    }

    private fun parseSeriesDoc(seriesUrl: String, doc: Document): MangaSeries {
        val title = doc.selectFirst(".series-title, h1.entry-title, .post-title, meta[property=og:title]")?.text()
            ?: doc.selectFirst("title")?.text()?.replace("- CrotPedia", "")?.trim()
            ?: "Unknown Manga Title"

        val coverUrl = doc.selectFirst(".series-thumb img, .thumb img, meta[property=og:image]")?.let {
            it.attr("abs:src").ifEmpty { it.attr("src") }
        } ?: ""

        val desc = doc.selectFirst(".series-synops, .entry-content p, .synopsis p")?.text()

        val chapterElements = doc.select(".chapter-list a, .eplister a, ul.cllist a, div#chapterlist a, .bxcl a")
        val rawChapters = if (chapterElements.isNotEmpty()) {
            chapterElements.mapNotNull { element ->
                val href = element.attr("abs:href").ifEmpty { element.attr("href") }
                val chTitle = element.text().trim()
                if (href.isNotEmpty() && chTitle.isNotEmpty()) {
                    href to chTitle
                } else null
            }
        } else {
            // Fallback to searching any link with chapter in path
            doc.select("a[href*=/baca/]").mapNotNull { element ->
                val href = element.attr("abs:href")
                val chTitle = element.text().trim()
                if (href.contains("/series/").not() && chTitle.isNotEmpty()) {
                    href to chTitle
                } else null
            }.distinctBy { it.first }
        }

        val chapterList = mutableListOf<MangaChapter>()
        // Parse order: oldest to newest or as listed
        rawChapters.reversed().forEachIndexed { index, (href, chTitle) ->
            val pageCount = fetchChapterPageCount(href)
            chapterList.add(
                MangaChapter(
                    title = chTitle,
                    chapterUrl = href,
                    pageCount = pageCount,
                    orderIndex = index + 1
                )
            )
        }

        if (chapterList.isEmpty()) {
            throw IllegalStateException("No chapters found on this series page.")
        }

        return MangaSeries(
            title = title,
            coverUrl = coverUrl,
            description = desc,
            seriesUrl = seriesUrl,
            chapters = chapterList
        )
    }

    private fun parseSingleChapterAsSeries(chapterUrl: String, doc: Document): MangaSeries {
        val chTitle = doc.selectFirst("h1.entry-title, .post-title, meta[property=og:title]")?.text()
            ?: doc.selectFirst("title")?.text()?.replace("- CrotPedia", "")?.trim()
            ?: "Chapter 1"

        val seriesTitle = doc.selectFirst(".allsub a, .breadcrumb a[href*=/series/]")?.text()
            ?: chTitle.replace(Regex("(?i)chapter\\s*\\d+.*"), "").trim().ifEmpty { chTitle }

        val coverUrl = doc.selectFirst("meta[property=og:image]")?.attr("content") ?: ""

        val pages = extractPagesFromChapterDoc(doc)
        val chapter = MangaChapter(
            title = chTitle,
            chapterUrl = chapterUrl,
            pageCount = pages.size,
            orderIndex = 1
        )

        return MangaSeries(
            title = seriesTitle,
            coverUrl = coverUrl,
            description = "Single Chapter Import",
            seriesUrl = chapterUrl,
            chapters = listOf(chapter)
        )
    }

    private fun fetchChapterPageCount(chapterUrl: String): Int {
        return try {
            val doc = fetchDocument(chapterUrl)
            extractPagesFromChapterDoc(doc).size
        } catch (e: Exception) {
            0
        }
    }

    fun parsePagesForChapter(chapterUrl: String): List<MangaPage> {
        val doc = fetchDocument(chapterUrl)
        return extractPagesFromChapterDoc(doc)
    }

    private fun extractPagesFromChapterDoc(doc: Document): List<MangaPage> {
        val imgElements = doc.select("#readerarea img, .reading-content img, .reader-area img, div#chpparent img, div.ts-main-image img")
        val fallbackImgs = if (imgElements.isEmpty()) {
            doc.select("img[src*=/wp-content/uploads/], img[data-src*=/wp-content/uploads/]")
        } else {
            imgElements
        }

        val pageList = mutableListOf<MangaPage>()
        var pageIndex = 1

        fallbackImgs.forEach { img ->
            var src = img.attr("abs:src")
            if (src.isBlank()) src = img.attr("data-src")
            if (src.isBlank()) src = img.attr("src")

            if (src.isNotBlank() && !src.contains("logo") && !src.contains("banner") && !src.contains("icon")) {
                if (src.startsWith("//")) {
                    src = "https:$src"
                }
                pageList.add(MangaPage(pageIndex = pageIndex++, imageUrl = src))
            }
        }

        return pageList
    }
}
