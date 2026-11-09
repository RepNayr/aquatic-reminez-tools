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
import java.math.BigDecimal
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class CrotPediaParser(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
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
                val parts = host.split(".")
                parts.size >= 2 && parts[parts.size - 2] == "crotpedia" && parts[parts.size - 1] == "net"
            } catch (e: Exception) {
                false
            }
        }

        fun isValidImageUrl(url: String): Boolean {
            val clean = url.trim()
            if (clean.isEmpty() || isPlaceholderUrl(clean) || isPromoBannerUrl(clean)) return false

            val uri = try { URI(clean) } catch (e: Exception) { return false }
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme != "http" && scheme != "https") return false
            val host = uri.host?.lowercase() ?: return false
            if (host.isEmpty()) return false

            val lower = clean.lowercase()
            if (lower.contains("logo") || lower.contains("banner") || lower.contains("advertisement") ||
                lower.contains("advert") || lower.contains("promo") || lower.contains("adserver") ||
                lower.contains("ad-server") || lower.contains("/ad/") || lower.contains("/ads/") ||
                lower.contains("/ads-") || lower.contains("square-ad") || lower.contains("-ad.") ||
                lower.contains("icon") || lower.contains("avatar") || lower.contains("spinner") ||
                lower.contains("loader") || lower.contains("facebook") || lower.contains("twitter") ||
                lower.contains("telegram") || lower.contains("discord") || lower.contains("donate") ||
                lower.contains("widget") || lower.contains("badge") || lower.contains("paypal") ||
                lower.contains("disqus") || lower.contains("gravatar")
            ) {
                return false
            }

            return true
        }

        private fun isPlaceholderUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.startsWith("data:image/") ||
                    lower.contains("placeholder") ||
                    lower.contains("loading") ||
                    lower.contains("blank") ||
                    lower.contains("spacer") ||
                    lower.isEmpty()
        }

        private fun isPromoBannerUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.contains("banner") || lower.contains("promo") || lower.contains("advert")
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
            } else if (response.code == 404) {
                throw MangaParseException("Chapter tidak ditemukan (HTTP 404).")
            } else if (response.code == 429) {
                throw MangaParseException("Terlalu banyak permintaan (HTTP 429). Silakan coba lagi beberapa saat lagi.")
            } else if (response.code >= 500) {
                throw MangaParseException("Server error (HTTP ${response.code}).")
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
        if (url.contains("/login/") || url.contains("action=login") || url.contains("/signin")) {
            throw MangaAuthRequiredException("Halaman memerlukan login resmi di CrotPedia.")
        }

        val loginForm = doc.selectFirst("form[action*=/login], form[action*=/signin], form#loginform, input[name=log], input[name=pwd]")
        if (loginForm != null) {
            throw MangaAuthRequiredException("Halaman ini memerlukan login resmi di CrotPedia.")
        }

        val title = doc.title().lowercase()
        if (title.contains("login") || title.contains("sign in") || title.contains("just a moment") || title.contains("attention required")) {
            throw MangaAuthRequiredException("Akses ditolak atau memerlukan login pada CrotPedia.")
        }
    }

    private fun extractMetaContent(doc: Document, propertyOrName: String): String? {
        val element = doc.selectFirst("meta[property=$propertyOrName], meta[name=$propertyOrName]")
        val content = element?.attr("content")?.trim()
        return if (!content.isNullOrEmpty()) content else null
    }

    internal fun extractCoverUrl(doc: Document): String {
        val coverSelectors = listOf(
            ".series-thumb img",
            ".thumb img",
            ".book img",
            ".infox .thumb img",
            ".post-thumbnail img",
            "div.thumb img",
            ".series-info img",
            ".series-details img",
            ".summary_image img"
        )
        for (selector in coverSelectors) {
            val img = doc.selectFirst(selector)
            if (img != null) {
                val url = extractImageUrlFromElement(doc, img)
                if (url.isNotEmpty() && isValidImageUrl(url)) {
                    return url
                }
            }
        }

        val ogImage = extractMetaContent(doc, "og:image")
        if (!ogImage.isNullOrEmpty() && isValidImageUrl(ogImage)) {
            return ogImage
        }

        val twitterImage = extractMetaContent(doc, "twitter:image")
        if (!twitterImage.isNullOrEmpty() && isValidImageUrl(twitterImage)) {
            return twitterImage
        }

        val generalImg = doc.selectFirst(".entry-content img, .series-synops img")
        if (generalImg != null) {
            val url = extractImageUrlFromElement(doc, generalImg)
            if (url.isNotEmpty() && isValidImageUrl(url)) {
                return url
            }
        }

        return ""
    }

    private fun canonicalizeChapterUrl(url: String): String {
        val clean = url.trim()
        val queryIndex = clean.indexOf('?')
        val hashIndex = clean.indexOf('#')
        val cutIndex = when {
            queryIndex != -1 && hashIndex != -1 -> minOf(queryIndex, hashIndex)
            queryIndex != -1 -> queryIndex
            hashIndex != -1 -> hashIndex
            else -> -1
        }
        val mainPath = if (cutIndex != -1) clean.substring(0, cutIndex) else clean
        val queryAndHash = if (cutIndex != -1) clean.substring(cutIndex) else ""

        val normalizedPath = if (mainPath.endsWith("/") && mainPath.length > 1 && !mainPath.endsWith("://")) {
            mainPath.dropLast(1)
        } else {
            mainPath
        }
        return normalizedPath + queryAndHash
    }

    private fun cleanChapterTitle(title: String): String {
        var cleaned = title
        val dateRegex1 = Regex("(?i)(?:January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},?\\s*\\d{4}")
        cleaned = dateRegex1.replace(cleaned, "")
        val dateRegex2 = Regex("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b")
        cleaned = dateRegex2.replace(cleaned, "")
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }

    internal fun parseSeriesDoc(seriesUrl: String, doc: Document): MangaSeries {
        val title = extractMetaContent(doc, "og:title")
            ?: doc.selectFirst(".series-title, h1.entry-title, .post-title")?.text()?.trim()
            ?: doc.title().replace("- CrotPedia", "").trim()
            .ifEmpty { "Judul Manga Tidak Diketahui" }

        val coverUrl = extractCoverUrl(doc)

        val desc = extractMetaContent(doc, "og:description")
            ?: doc.selectFirst(".series-synops, .entry-content p, .synopsis p")?.text()?.trim()

        val chapterElements = doc.select(".chapter-list a, .eplister a, ul.cllist a, div#chapterlist a, .bxcl a, .clist ul li a, .episodelist ul li a, .allc a")
        val candidateElements = if (chapterElements.isNotEmpty()) {
            chapterElements
        } else {
            doc.select("a[href*=/baca/]")
        }

        val forbiddenPathKeywords = listOf("/series/", "/tag/", "/category/", "/genre/", "/author/", "/page/", "/search/", "request", "wp-login", "wp-admin", "comment")
        val forbiddenTitleKeywords = listOf("request list", "request", "ahegao", "big breast", "blowjob", "fingerings", "genre", "tag", "author", "category", "contact", "about", "privacy")

        val rawChapters = mutableListOf<Pair<String, String>>()
        val seenNormalizedUrls = mutableSetOf<String>()

        candidateElements.forEach { element ->
            val href = element.attr("abs:href").ifEmpty { resolveUrlCandidate(doc.baseUri(), element.attr("href")) }
            if (href.isEmpty() || !isAllowedUrl(href)) return@forEach

            val lowerHref = href.lowercase()
            if (forbiddenPathKeywords.any { lowerHref.contains(it) }) return@forEach

            if (!lowerHref.contains("/baca/") && !lowerHref.matches(Regex(".*-\\d+(?:\\.\\d+)?/?$"))) {
                return@forEach
            }

            val clone = element.clone()
            clone.select("time, .date, .chapter-date, span.date, span.dt, .dt").remove()
            var chTitle = clone.text().trim()

            chTitle = cleanChapterTitle(chTitle)
            if (chTitle.isEmpty()) return@forEach

            val lowerTitle = chTitle.lowercase()
            if (forbiddenTitleKeywords.any { lowerTitle.contains(it) }) return@forEach

            val canonicalUrl = canonicalizeChapterUrl(href)
            if (seenNormalizedUrls.add(canonicalUrl)) {
                rawChapters.add(canonicalUrl to chTitle)
            }
        }

        if (rawChapters.isEmpty()) {
            throw MangaParseException("Tidak ada chapter yang ditemukan pada halaman series ini.")
        }

        // Preserve original website chapter order as scraped from DOM
        val chapterList = rawChapters.mapIndexed { index, (href, chTitle) ->
            MangaChapter(
                title = chTitle,
                chapterUrl = href,
                pageCount = -1,
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

        val coverUrl = extractCoverUrl(doc)
        val pages = extractPagesFromChapterDoc(doc)

        val chapter = MangaChapter(
            title = chTitle,
            chapterUrl = chapterUrl,
            pageCount = pages.size,
            orderIndex = 1,
            pages = pages.map { it.imageUrl }
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
        val readerContainer = doc.selectFirst("#readerarea, .reading-content, .reader-area, div#chpparent, div.ts-main-image, .entry-content, .post-body")

        val imgElements = doc.select("#readerarea img, .reading-content img, .reader-area img, div#chpparent img, div.ts-main-image img, .entry-content img")
        val candidateElements = if (imgElements.isNotEmpty()) {
            imgElements
        } else {
            doc.select("img[src*=/wp-content/uploads/], img[data-src*=/wp-content/uploads/], img[data-original*=/wp-content/uploads/]")
        }

        val pageList = mutableListOf<MangaPage>()
        val seenUrls = mutableSetOf<String>()
        var pageIndex = 1

        candidateElements.forEach { img ->
            // Skip images inside ad/sidebar/header/footer containers
            var parent: org.jsoup.nodes.Element? = img.parent()
            var isAdOrNavigation = false
            while (parent != null) {
                val classOrId = (parent.className() + " " + parent.id()).lowercase()
                if (classOrId.contains("ad-") || classOrId.contains("banner") || classOrId.contains("sidebar") ||
                    classOrId.contains("comment") || classOrId.contains("footer") || classOrId.contains("header") ||
                    classOrId.contains("social") || classOrId.contains("nav")
                ) {
                    isAdOrNavigation = true
                    break
                }
                parent = parent.parent()
            }
            if (isAdOrNavigation) return@forEach

            val src = extractImageUrlFromElement(doc, img)

            if (src.isNotBlank() && isValidImageUrl(src) && seenUrls.add(src)) {
                pageList.add(MangaPage(pageIndex = pageIndex++, imageUrl = src))
            }
        }

        if (pageList.isEmpty()) {
            if (readerContainer == null) {
                throw MangaParseException("Reader container not detected")
            } else {
                throw MangaParseException("No valid images found")
            }
        }

        return pageList
    }

    private fun extractImageUrlFromElement(doc: Document, img: org.jsoup.nodes.Element): String {
        val attrs = listOf(
            "data-src", "data-lazy-src", "data-original", "data-lazy", "data-url", "src"
        )
        for (attr in attrs) {
            val value = img.attr(attr).trim()
            if (value.isNotEmpty() && !isPlaceholderUrl(value)) {
                val clean = resolveUrlCandidate(doc.baseUri(), value)
                if (clean.isNotEmpty() && isValidImageUrl(clean)) return clean
            }
        }

        val srcset = img.attr("srcset").ifEmpty { img.attr("data-srcset") }.trim()
        if (srcset.isNotEmpty()) {
            val candidates = srcset.split(",")
            for (candidate in candidates) {
                val parts = candidate.trim().split(Regex("\\s+"))
                val urlPart = parts.firstOrNull()?.trim()
                if (!urlPart.isNullOrEmpty() && !isPlaceholderUrl(urlPart)) {
                    val clean = resolveUrlCandidate(doc.baseUri(), urlPart)
                    if (clean.isNotEmpty() && isValidImageUrl(clean)) return clean
                }
            }
        }

        return ""
    }

    private fun resolveUrlCandidate(baseUri: String, url: String): String {
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }
        return try {
            if (baseUri.isNotEmpty()) {
                URI(baseUri).resolve(trimmed).toString()
            } else {
                trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    internal fun parseChapterNumber(title: String): BigDecimal? {
        val chMatcher = CHAPTER_NUM_PATTERN.matcher(title)
        if (chMatcher.find()) {
            val numStr = chMatcher.group(1)
            if (numStr != null) {
                return try { BigDecimal(numStr) } catch (_: Exception) { null }
            }
        }
        val genMatcher = GENERIC_NUM_PATTERN.matcher(title)
        if (genMatcher.find()) {
            val numStr = genMatcher.group(1)
            if (numStr != null) {
                return try { BigDecimal(numStr) } catch (_: Exception) { null }
            }
        }
        return null
    }
}
