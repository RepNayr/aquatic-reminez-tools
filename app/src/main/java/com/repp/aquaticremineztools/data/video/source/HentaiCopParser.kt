package com.repp.aquaticremineztools.data.video.source

import com.repp.aquaticremineztools.data.video.model.ResolvedVideoSource
import com.repp.aquaticremineztools.data.video.model.VideoEpisode
import com.repp.aquaticremineztools.data.video.model.VideoQuality
import com.repp.aquaticremineztools.data.video.model.VideoSeries
import com.repp.aquaticremineztools.data.video.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class HentaiCopParser(
    context: android.content.Context? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (context != null) {
                cookieJar(com.repp.aquaticremineztools.data.manga.source.CrotPediaCookieJar(context))
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()
) {

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val DURATION_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})")

        fun isAllowedSeriesUrl(url: String): Boolean {
            return try {
                val clean = canonicalizeUrl(url)
                val uri = URI(clean)
                val scheme = uri.scheme?.lowercase() ?: return false
                if (scheme != "http" && scheme != "https") return false

                val host = uri.host?.lowercase() ?: return false
                val isHentaiCop = host == "hentaicop.com" || host.endsWith(".hentaicop.com")
                isHentaiCop && (uri.path.contains("/series/") || uri.path.contains("/series"))
            } catch (_: Exception) {
                false
            }
        }

        fun canonicalizeUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return ""
            return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }
    }

    suspend fun parseSeries(seriesUrl: String): VideoSeries = withContext(Dispatchers.IO) {
        val canonical = canonicalizeUrl(seriesUrl)
        require(isAllowedSeriesUrl(canonical)) {
            "URL tidak valid. Masukkan URL HentaiCop series yang valid, misal: https://hentaicop.com/series/hitori-tsuma/"
        }

        val html = fetchHtml(canonical)
        val doc = Jsoup.parse(html, canonical)

        val title = doc.selectFirst("h1.ht-series__title")?.text()?.trim()
            ?: doc.selectFirst("meta[property='og:title']")?.attr("content")?.replace("- Hentaicop", "")?.trim()
            ?: "Unknown Series"

        val coverUrl = doc.selectFirst("div.ht-series__poster img")?.attr("src")
            ?: doc.selectFirst("meta[property='og:image']")?.attr("content")
            ?: ""

        val description = doc.selectFirst("div.ht-synopsis p")?.text()?.trim()
            ?: doc.selectFirst("div.ht-synopsis")?.text()?.trim()
            ?: doc.selectFirst("meta[property='og:description']")?.attr("content")?.trim()
            ?: "Tidak ada deskripsi."

        val episodes = mutableListOf<VideoEpisode>()
        val epElements = doc.select("ol.ht-eplist__list li, div.ht-eplist li")

        var fallbackOrder = epElements.size
        for (element in epElements) {
            val link = element.selectFirst("a.ht-eplist__item, a") ?: continue
            val epUrl = link.absUrl("href")
            if (epUrl.isBlank()) continue

            val epTitle = element.selectFirst("span.ht-eplist__name")?.text()?.trim()
                ?: link.text().trim()

            val numAttr = element.attr("data-num").ifBlank {
                element.selectFirst("span.ht-eplist__num")?.text()?.trim() ?: ""
            }
            val parsedNum = numAttr.toIntOrNull() ?: run {
                val matcher = Pattern.compile("(?i)episode\\s*(\\d+)").matcher(epTitle)
                if (matcher.find()) matcher.group(1)?.toIntOrNull() else null
            }
            val realOrder = parsedNum ?: (fallbackOrder--)

            episodes.add(
                VideoEpisode(
                    id = epUrl,
                    title = epTitle,
                    url = epUrl,
                    order = realOrder
                )
            )
        }

        if (episodes.isEmpty()) {
            // Fallback: check if single page episode link exists
            val mainActionLink = doc.selectFirst("a.ht-btn--grad")?.absUrl("href")
            if (!mainActionLink.isNullOrBlank() && !mainActionLink.contains("/series/")) {
                episodes.add(
                    VideoEpisode(
                        id = mainActionLink,
                        title = "$title Episode 1",
                        url = mainActionLink,
                        order = 1
                    )
                )
            }
        }

        VideoSeries(
            id = canonical,
            title = title,
            coverUrl = canonicalizeUrl(coverUrl),
            description = description,
            episodes = episodes
        )
    }

    suspend fun parseEpisodeDetails(episodeUrl: String): VideoEpisode = withContext(Dispatchers.IO) {
        val canonical = canonicalizeUrl(episodeUrl)
        val html = fetchHtml(canonical)
        val doc = Jsoup.parse(html, canonical)

        val title = doc.selectFirst("h1.hcu-title")?.text()?.trim()
            ?: doc.selectFirst("h1.ht-series__title")?.text()?.trim()
            ?: doc.selectFirst("meta[property='og:title']")?.attr("content")?.replace("- Hentaicop", "")?.trim()
            ?: "Episode"

        val thumbnailUrl = doc.selectFirst("div.hcu-thumb img")?.absUrl("src")?.ifBlank {
            doc.selectFirst("div.hcu-thumb img")?.attr("src")
        } ?: doc.selectFirst("meta[property='og:image']")?.attr("content")
        ?: ""

        var durationFormatted = ""
        val chips = doc.select("ul.hcu-chips li")
        for (chip in chips) {
            val text = chip.text()
            val matcher = DURATION_PATTERN.matcher(text)
            if (matcher.find()) {
                durationFormatted = matcher.group(1) ?: ""
                break
            }
        }

        val sources = mutableListOf<VideoSource>()
        val dlRows = doc.select("section.ht-dl tbody tr")

        for (row in dlRows) {
            val serverName = row.selectFirst("td.ht-dl__host")?.text()?.trim() ?: "HentaiCop"
            val qualityLabel = row.selectFirst("td.ht-dl__q")?.text()?.trim() ?: "720p"
            val sizeStr = row.selectFirst("td.ht-dl__size")?.text()?.trim() ?: ""
            val dlPageUrl = row.selectFirst("a.no-popunder")?.absUrl("href") ?: ""

            if (dlPageUrl.isNotBlank()) {
                val height = parseQualityHeight(qualityLabel)
                val sizeBytes = parseSizeBytes(sizeStr)

                sources.add(
                    VideoSource(
                        serverId = "hentaicop_${serverName.lowercase().replace(" ", "_")}_$qualityLabel",
                        serverName = serverName,
                        quality = VideoQuality(
                            label = qualityLabel,
                            height = height,
                            fileSizeBytes = sizeBytes,
                            formattedSize = sizeStr
                        ),
                        downloadPageUrl = dlPageUrl,
                        mediaFormat = "MP4",
                        refererHeader = canonical
                    )
                )
            }
        }

        if (sources.isEmpty()) {
            // Fallback: check hcu-file elements
            val hcuFiles = doc.select("div.hcu-file")
            for (file in hcuFiles) {
                val qLabel = file.attr("data-q").ifBlank { "720p" }
                val sizeStr = file.selectFirst("span.hcu-file__meta")?.text() ?: ""
                val dlPageUrl = file.selectFirst("a.hcu-btn")?.absUrl("href") ?: canonical

                sources.add(
                    VideoSource(
                        serverId = "hentaicop_main_$qLabel",
                        serverName = "HentaiCop",
                        quality = VideoQuality(
                            label = qLabel,
                            height = parseQualityHeight(qLabel),
                            fileSizeBytes = parseSizeBytes(sizeStr),
                            formattedSize = sizeStr
                        ),
                        downloadPageUrl = dlPageUrl,
                        mediaFormat = "MP4",
                        refererHeader = canonical
                    )
                )
            }
        }

        VideoEpisode(
            id = canonical,
            title = title,
            url = canonical,
            thumbnailUrl = canonicalizeUrl(thumbnailUrl),
            durationFormatted = durationFormatted,
            availableSources = sources
        )
    }

    suspend fun resolveDirectMediaSource(source: VideoSource): ResolvedVideoSource = withContext(Dispatchers.IO) {
        val dlPageUrl = source.downloadPageUrl
        require(dlPageUrl.isNotBlank()) { "Download page URL is blank." }

        // Session warm-up: visit episode referer page first to populate session cookies
        val refererUrl = if (source.refererHeader.isNotBlank()) source.refererHeader else "https://hentaicop.com/"
        if (refererUrl.startsWith("http")) {
            try {
                val warmupReq = Request.Builder()
                    .url(refererUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(warmupReq).execute().close()
            } catch (_: Exception) {}
        }

        // Step 1: Fetch download page html
        val request = Request.Builder()
            .url(dlPageUrl)
            .header("User-Agent", USER_AGENT)
            .header("Referer", refererUrl)
            .build()

        var responseUrl = dlPageUrl
        val responseHtml = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} saat membuka halaman unduh")
            responseUrl = resp.request.url.toString()
            resp.body?.string() ?: throw IOException("Respons halaman unduh kosong")
        }

        val doc = Jsoup.parse(responseHtml, responseUrl)

        // Step 2: Extract go link
        val goLinkElem = doc.selectFirst("a[data-hcu-go]")
        val rawGoUrl = goLinkElem?.absUrl("href") ?: run {
            val matcher = Pattern.compile("href=[\"']([^\"']*\\?go=[^\"']*)[\"']").matcher(responseHtml)
            if (matcher.find()) {
                val found = matcher.group(1) ?: ""
                try {
                    URI(responseUrl).resolve(found).toString()
                } catch (_: Exception) {
                    if (found.startsWith("http")) found else "https://hentaicop.com$found"
                }
            } else {
                ""
            }
        }

        val goUrl = rawGoUrl.replace("&#038;", "&").replace("&amp;", "&")

        require(goUrl.isNotBlank()) { "Tautan unduh (parameter 'go') tidak ditemukan di halaman." }

        // Step 3: Fetch go link with followRedirects disabled
        val noRedirectClient = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        val goRequest = Request.Builder()
            .url(goUrl)
            .header("User-Agent", USER_AGENT)
            .header("Referer", dlPageUrl)
            .build()

        val mediaUrl = noRedirectClient.newCall(goRequest).execute().use { goResp ->
            val location = goResp.header("Location")
            if (!location.isNullOrBlank()) {
                val cleanLoc = location.replace("&#038;", "&").replace("&amp;", "&")
                try {
                    URI(goUrl).resolve(cleanLoc).toString()
                } catch (_: Exception) {
                    cleanLoc
                }
            } else if (goResp.isSuccessful) {
                goResp.request.url.toString()
            } else {
                throw IOException("Gagal mendapatkan lokasi media (HTTP ${goResp.code})")
            }
        }

        require(mediaUrl.isNotBlank() && mediaUrl != dlPageUrl && !mediaUrl.contains(".html")) {
            "URL media yang diselesaikan tidak valid atau merujuk ke halaman HTML: $mediaUrl"
        }

        ResolvedVideoSource(
            mediaUrl = mediaUrl,
            referer = dlPageUrl,
            userAgent = USER_AGENT,
            expectedSizeBytes = source.quality.fileSizeBytes,
            selectedQuality = source.quality
        )
    }

    suspend fun resolveDirectMediaUrl(source: VideoSource): String = withContext(Dispatchers.IO) {
        resolveDirectMediaSource(source).mediaUrl
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Gagal mengakses URL (HTTP ${response.code})")
            }
            return response.body?.string() ?: throw IOException("Respons HTML kosong")
        }
    }

    private fun parseQualityHeight(qualityLabel: String): Int {
        val clean = qualityLabel.lowercase()
        return when {
            clean.contains("1080") -> 1080
            clean.contains("720") -> 720
            clean.contains("480") -> 480
            clean.contains("360") -> 360
            else -> 720
        }
    }

    private fun parseSizeBytes(sizeStr: String): Long {
        if (sizeStr.isBlank()) return 0L
        val lower = sizeStr.lowercase().trim()
        val numMatch = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(lower)
        if (!numMatch.find()) return 0L

        val num = numMatch.group(1)?.toDoubleOrNull() ?: return 0L
        return when {
            lower.contains("gb") -> (num * 1024 * 1024 * 1024).toLong()
            lower.contains("mb") -> (num * 1024 * 1024).toLong()
            lower.contains("kb") -> (num * 1024).toLong()
            else -> num.toLong()
        }
    }
}
