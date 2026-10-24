package com.repp.aquaticremineztools.data.remote.news

import android.util.Log
import android.util.Xml
import com.repp.aquaticremineztools.data.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class RssSourceConfig(
    val url: String,
    val publisher: String,
    val category: String,
    val language: String
)

class RssFeedService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "RssFeedService"

        val RSS_SOURCES = listOf(
            // Indonesian Feeds
            RssSourceConfig("https://www.antaranews.com/rss/tekno.xml", "Antara News", "Technology", "id"),
            RssSourceConfig("https://www.antaranews.com/rss/olahraga.xml", "Antara News", "Sports", "id"),
            RssSourceConfig("https://www.antaranews.com/rss/ekonomi.xml", "Antara News", "Business", "id"),
            RssSourceConfig("https://www.antaranews.com/rss/gaya-hidup.xml", "Antara News", "Lifestyle", "id"),
            RssSourceConfig("https://www.antaranews.com/rss/otomotif.xml", "Antara News", "Automotive", "id"),
            RssSourceConfig("https://www.antaranews.com/rss/hiburan.xml", "Antara News", "Entertainment", "id"),
            RssSourceConfig("https://www.republika.co.id/rss/tekno", "Republika", "Technology", "id"),
            RssSourceConfig("https://www.cnnindonesia.com/teknologi/rss", "CNN Indonesia", "Technology", "id"),
            RssSourceConfig("https://www.cnnindonesia.com/olahraga/rss", "CNN Indonesia", "Sports", "id"),

            // English Feeds
            RssSourceConfig("http://feeds.bbci.co.uk/news/technology/rss.xml", "BBC News", "Technology", "en"),
            RssSourceConfig("http://feeds.bbci.co.uk/news/business/rss.xml", "BBC News", "Business", "en"),
            RssSourceConfig("http://feeds.bbci.co.uk/news/science_and_environment/rss.xml", "BBC News", "Health", "en"),
            RssSourceConfig("http://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml", "BBC News", "Entertainment", "en"),
            RssSourceConfig("https://techcrunch.com/feed/", "TechCrunch", "Technology", "en"),
            RssSourceConfig("https://www.wired.com/feed/rss", "Wired", "Technology", "en")
        )
    }

    suspend fun fetchFeed(config: RssSourceConfig): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        try {
            val request = Request.Builder()
                .url(config.url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.w(TAG, "Failed HTTP response from ${config.url}: ${response.code}")
                return@withContext emptyList()
            }

            val xmlData = response.body!!.string()
            val parsedArticles = parseRssXml(xmlData, config)
            articles.addAll(parsedArticles)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching feed from ${config.url}: ${e.message}")
        }
        articles
    }

    private fun parseRssXml(xml: String, config: RssSourceConfig): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false

            var title = ""
            var description = ""
            var content = ""
            var link = ""
            var pubDate = ""
            var thumbnailUrl: String? = null
            var itemCategory: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            inItem = true
                            title = ""
                            description = ""
                            content = ""
                            link = ""
                            pubDate = ""
                            thumbnailUrl = null
                            itemCategory = null
                        } else if (inItem) {
                            when {
                                name.equals("title", ignoreCase = true) -> {
                                    title = readText(parser)
                                }
                                name.equals("description", ignoreCase = true) || name.equals("summary", ignoreCase = true) -> {
                                    description = readText(parser)
                                }
                                name.equals("content", ignoreCase = true) || name.equals("content:encoded", ignoreCase = true) -> {
                                    content = readText(parser)
                                }
                                name.equals("link", ignoreCase = true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    link = if (!href.isNullOrBlank()) href else readText(parser)
                                }
                                name.equals("pubDate", ignoreCase = true) || name.equals("published", ignoreCase = true) || name.equals("updated", ignoreCase = true) -> {
                                    pubDate = readText(parser)
                                }
                                name.equals("category", ignoreCase = true) -> {
                                    val catText = readText(parser)
                                    if (catText.isNotBlank()) {
                                        itemCategory = mapItemCategory(catText, config.category)
                                    }
                                }
                                name.equals("enclosure", ignoreCase = true) || name.equals("media:content", ignoreCase = true) || name.equals("media:thumbnail", ignoreCase = true) -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    val type = parser.getAttributeValue(null, "type")
                                    if (!url.isNullOrBlank() && (type == null || type.startsWith("image"))) {
                                        thumbnailUrl = url
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            inItem = false

                            val cleanTitle = stripHtml(title).trim()
                            val cleanDesc = stripHtml(if (description.isNotBlank()) description else content).trim()
                            val cleanLink = link.trim()

                            if (thumbnailUrl.isNullOrBlank()) {
                                thumbnailUrl = extractImgSrc(description) ?: extractImgSrc(content)
                            }

                            val publishedEpoch = parseDateToEpoch(pubDate)
                            val finalCategory = itemCategory ?: config.category

                            if (cleanTitle.isNotBlank() && cleanLink.isNotBlank()) {
                                val article = NewsArticle(
                                    id = cleanLink,
                                    headline = cleanTitle,
                                    summary = if (cleanDesc.length > 200) cleanDesc.substring(0, 200) + "…" else cleanDesc,
                                    content = if (content.isNotBlank()) content else description,
                                    thumbnailUrl = thumbnailUrl,
                                    articleUrl = cleanLink,
                                    publisher = config.publisher,
                                    category = finalCategory,
                                    publishedAtEpoch = publishedEpoch,
                                    language = config.language
                                )
                                articles.add(article)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML parse error for ${config.url}: ${e.message}")
        }
        return articles
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("\\s+".toRegex(), " ")
    }

    private fun extractImgSrc(html: String): String? {
        if (html.isBlank()) return null
        val pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val url = matcher.group(1)
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                return url
            }
        }
        return null
    }

    private fun parseDateToEpoch(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()

        val formats = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(dateStr)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    private fun mapItemCategory(rawCat: String, defaultCat: String): String {
        val catLower = rawCat.lowercase(Locale.ROOT)
        return when {
            catLower.contains("tech") || catLower.contains("teknologi") -> "Technology"
            catLower.contains("sport") || catLower.contains("olahraga") -> "Sports"
            catLower.contains("business") || catLower.contains("ekonomi") || catLower.contains("bisnis") -> "Business"
            catLower.contains("life") || catLower.contains("gaya hidup") -> "Lifestyle"
            catLower.contains("health") || catLower.contains("sehat") || catLower.contains("kesehatan") -> "Health"
            catLower.contains("auto") || catLower.contains("otomotif") -> "Automotive"
            catLower.contains("entertain") || catLower.contains("hiburan") || catLower.contains("art") -> "Entertainment"
            else -> defaultCat
        }
    }
}
