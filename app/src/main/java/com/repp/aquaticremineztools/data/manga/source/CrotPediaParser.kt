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
import java.util.regex.Pattern

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

        private val CHAPTER_NUM_PATTERN = Pattern.compile("(?i)chapter\\s*(\\d+(?:\\.\\d+)?)")
        private val GENERIC_NUM_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)")

        fun isAllowedUrl(url: String): Boolean {
            return try {
                val uri = URI(url.trim())
                val scheme = uri.scheme?.lowercase() ?: return false
                if (scheme != "http" && scheme != "https") return false

                val host = uri.host?.lowercase() ?: return false
                host == "crotpedia.net" || host == "www.crotpedia.net" || host.endsWith(".crotpedia.net")
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun parseUrl(inputUrl: String): MangaSeries = withContext(Dispatchers.IO) {
        val cleanUrl = inputUrl.trim()
        if (!isAllowedUrl(cleanUrl)) {
            throw IllegalArgumentException("URL harus berasal dari domain crotpedia.net yang sah.")
        }

        val doc = fetchDocument(cleanUrl)

        if (cleanUrl.contains("/series/")) {
            parseSeriesDoc(cleanUrl, doc)
        } else {
            parseSingleChapterAsSeries(cleanUrl, doc)
        }
    }

    internal fun fetchDocument(url: String): Document {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://crotpedia.net/")
            .build()

        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            if (!isAllowedUrl(finalUrl)) {
                throw MangaParseException("Pengalihan ke domain tidak dipercaya ditolak.")
            }

            if (response.code == 401 || response.code == 403) {
                throw MangaAuthRequiredException("Akses ditolak (HTTP ${response.code}). Konten memerlukan autentikasi login.")
            } else if (response.code == 429) {
                throw MangaParseException("Terlalu banyak permintaan (HTTP 429). Silakan coba lagi beberapa saat lagi.")
            } else if (!response.isSuccessful) {
                throw MangaParseException("Gagal memuat halaman: HTTP ${response.code}")
            }

            val html = response.body?.string()
                ?: throw MangaParseException("Respons kosong dari server.")

            val doc = Jsoup.parse(html, finalUrl)
            validateNotLoginPage(doc, finalUrl)
            return doc
        }
    }

    private fun validateNotLoginPage(doc: Document, url: String) {
        if (url.contains("/login/") || url.contains("action=login")) {
            throw MangaAuthRequiredException("Halaman memerlukan login resmi di CrotPedia.")
        }

        val loginForm = doc.selectFirst("form[action*=/login], form#loginform, input[name=log], input[name=pwd]")
        if (loginForm != null) {
            throw MangaAuthRequiredException("Halaman ini memerlukan login resmi di CrotPedia.")
        }

        val title = doc.title().lowercase()
        if (title.contains("login") || title.contains("sign in") || title.contains("just a moment")) {
            throw MangaAuthRequiredException("Akses ditolak atau memerlukan login pada CrotPedia.")
        }
    }

    private fun extractMetaContent(doc: Document, propertyOrName: String): String? {
        val element = doc.selectFirst("meta[property=$propertyOrName], meta[name=$propertyOrName]")
        val content = element?.attr("content")?.trim()
        return if (!content.isNullOrEmpty()) content else null
    }

    private fun parseSeriesDoc(seriesUrl: String, doc: Document): MangaSeries {
        val title = extractMetaContent(doc, "og:title")
            ?: doc.selectFirst(".series-title, h1.entry-title, .post-title")?.text()?.trim()
            ?: doc.title().replace("- CrotPedia", "").trim()
            .ifEmpty { "Judul Manga Tidak Diketahui" }

        val coverUrl = extractMetaContent(doc, "og:image")
            ?: doc.selectFirst(".series-thumb img, .thumb img, .entry-content img")?.let {
                extractImageUrlFromElement(it)
            } ?: ""

        val desc = extractMetaContent(doc, "og:description")
            ?: doc.selectFirst(".series-synops, .entry-content p, .synopsis p")?.text()?.trim()

        val chapterElements = doc.select(".chapter-list a, .eplister a, ul.cllist a, div#chapterlist a, .bxcl a")
        val rawChapters = if (chapterElements.isNotEmpty()) {
            chapterElements.mapNotNull { element ->
                val href = element.attr("abs:href").ifEmpty { element.attr("href") }
                val chTitle = element.text().trim()
                if (href.isNotEmpty() && chTitle.isNotEmpty() && !href.contains("/series/")) {
                    href to chTitle
                } else null
            }
        } else {
            doc.select("a[href*=/baca/]").mapNotNull { element ->
                val href = element.attr("abs:href")
                val chTitle = element.text().trim()
                if (href.isNotEmpty() && !href.contains("/series/") && chTitle.isNotEmpty()) {
                    href to chTitle
                } else null
            }.distinctBy { it.first }
        }

        if (rawChapters.isEmpty()) {
            throw MangaParseException("Tidak ada chapter yang ditemukan pada halaman series ini.")
        }

        val parsedChapters = rawChapters.map { (href, chTitle) ->
            val num = parseChapterNumber(chTitle)
            Triple(href, chTitle, num)
        }

        // Sort by chapter number numerically ascending if numbers parsed, else keep DOM order
        val sortedChapters = if (parsedChapters.any { it.third != null }) {
            parsedChapters.sortedWith(Comparator { a, b ->
                val numA = a.third
                val numB = b.third
                if (numA != null && numB != null) {
                    numA.compareTo(numB)
                } else if (numA != null) -1 else 1
            })
        } else {
            // Check if DOM order is descending (Chapter N down to 1), reverse if so
            parsedChapters.reversed()
        }

        val chapterList = sortedChapters.mapIndexed { index, (href, chTitle, _) ->
            MangaChapter(
                title = chTitle,
                chapterUrl = href,
                pageCount = 0, // Page count loaded on demand
                orderIndex = index + 1
            )
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
        val chTitle = extractMetaContent(doc, "og:title")
            ?: doc.selectFirst("h1.entry-title, .post-title")?.text()?.trim()
            ?: doc.title().replace("- CrotPedia", "").trim()
            .ifEmpty { "Chapter 1" }

        val seriesTitle = doc.selectFirst(".allsub a, .breadcrumb a[href*=/series/]")?.text()?.trim()
            ?: chTitle.replace(Regex("(?i)chapter\\s*\\d+.*"), "").trim().ifEmpty { chTitle }

        val coverUrl = extractMetaContent(doc, "og:image") ?: ""
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
            description = "Impor Single Chapter",
            seriesUrl = chapterUrl,
            chapters = listOf(chapter)
        )
    }

    fun parsePagesForChapter(chapterUrl: String): List<MangaPage> {
        val doc = fetchDocument(chapterUrl)
        return extractPagesFromChapterDoc(doc)
    }

    private fun extractPagesFromChapterDoc(doc: Document): List<MangaPage> {
        val imgElements = doc.select("#readerarea img, .reading-content img, .reader-area img, div#chpparent img, div.ts-main-image img")
        val candidateElements = if (imgElements.isNotEmpty()) {
            imgElements
        } else {
            doc.select("img[src*=/wp-content/uploads/], img[data-src*=/wp-content/uploads/]")
        }

        val pageList = mutableListOf<MangaPage>()
        val seenUrls = mutableSetOf<String>()
        var pageIndex = 1

        candidateElements.forEach { img ->
            val src = extractImageUrlFromElement(img)

            if (src.isNotBlank() && isMangaPageImage(src) && seenUrls.add(src)) {
                pageList.add(MangaPage(pageIndex = pageIndex++, imageUrl = src))
            }
        }

        return pageList
    }

    private fun extractImageUrlFromElement(img: org.jsoup.nodes.Element): String {
        val attrs = listOf(
            "abs:src", "data-src", "data-lazy-src", "data-original", "data-url", "src"
        )
        for (attr in attrs) {
            val value = img.attr(attr).trim()
            if (value.isNotEmpty()) {
                val clean = resolveUrlCandidate(value)
                if (clean.isNotEmpty()) return clean
            }
        }

        val srcset = img.attr("srcset").ifEmpty { img.attr("data-srcset") }.trim()
        if (srcset.isNotEmpty()) {
            val firstCandidate = srcset.split(",").firstOrNull()?.trim()?.split(" ")?.firstOrNull()
            if (!firstCandidate.isNullOrEmpty()) {
                return resolveUrlCandidate(firstCandidate)
            }
        }

        return ""
    }

    private fun resolveUrlCandidate(url: String): String {
        var clean = url.trim()
        if (clean.startsWith("//")) {
            clean = "https:$clean"
        }
        return clean
    }

    private fun isMangaPageImage(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains("logo") || lower.contains("banner") || lower.contains("icon") ||
            lower.contains("avatar") || lower.contains("advertisement") || lower.contains("button")
        ) {
            return false
        }
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.contains("/uploads/")
    }

    internal fun parseChapterNumber(title: String): Float? {
        val chMatcher = CHAPTER_NUM_PATTERN.matcher(title)
        if (chMatcher.find()) {
            return chMatcher.group(1)?.toFloatOrNull()
        }
        val genMatcher = GENERIC_NUM_PATTERN.matcher(title)
        if (genMatcher.find()) {
            return genMatcher.group(1)?.toFloatOrNull()
        }
        return null
    }
}
