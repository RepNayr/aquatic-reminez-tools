package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CrotPediaParserUnitTest {

    private val parser = CrotPediaParser()

    @Test
    fun testAllowedUrlStrictValidation() {
        assertTrue(CrotPediaParser.isAllowedUrl("https://crotpedia.net/baca/series/test-series/"))
        assertTrue(CrotPediaParser.isAllowedUrl("http://www.crotpedia.net/baca/chapter-1/"))
        assertTrue(CrotPediaParser.isAllowedUrl("https://sub.crotpedia.net/baca/chapter-1/"))

        assertFalse(CrotPediaParser.isAllowedUrl("https://crotpedia.net.evil.com/baca/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://evil.com/crotpedia.net/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://evilcrotpedia.net/baca/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://crotpedia-net.com/baca/"))
        assertFalse(CrotPediaParser.isAllowedUrl("ftp://crotpedia.net/file"))
        assertFalse(CrotPediaParser.isAllowedUrl("invalid-url"))
    }

    @Test
    fun testIsValidImageUrlSupportsExternalCdnAndFiltersBanners() {
        assertTrue(CrotPediaParser.isValidImageUrl("https://i0.wp.com/crotpedia.net/cover.jpg"))
        assertTrue(CrotPediaParser.isValidImageUrl("https://cdn.manga.org/images/chapter1/01.webp"))
        assertTrue(CrotPediaParser.isValidImageUrl("http://images.cdn.com/page.png"))

        assertFalse(CrotPediaParser.isValidImageUrl("https://crotpedia.net/logo.png"))
        assertFalse(CrotPediaParser.isValidImageUrl("https://crotpedia.net/wp-content/uploads/promo-banner.jpg"))
        assertFalse(CrotPediaParser.isValidImageUrl("https://crotpedia.net/ads/square-ad.gif"))
        assertFalse(CrotPediaParser.isValidImageUrl("data:image/svg+xml;base64,123"))
        assertFalse(CrotPediaParser.isValidImageUrl(""))
    }

    @Test
    fun testParseChapterNumberExtraction() {
        assertEquals(BigDecimal("1"), parser.parseChapterNumber("Chapter 1"))
        assertEquals(BigDecimal("2.5"), parser.parseChapterNumber("Musume no Tomodachi Chapter 2.5 Bahasa Indonesia"))
        assertEquals(BigDecimal("10"), parser.parseChapterNumber("Chapter 10 - End"))
        assertEquals(BigDecimal("100"), parser.parseChapterNumber("Vol 2 Chapter 100"))
        assertNull(parser.parseChapterNumber("Extra Story Special"))
    }

    @Test
    fun testParseSeriesHtmlOgMetadata() {
        val sampleSeriesHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Manga Series - CrotPedia</title>
                <meta property="og:title" content="Meta Title Manga" />
                <meta property="og:image" content="https://i0.wp.com/crotpedia.net/wp-content/uploads/cover.jpg" />
                <meta property="og:description" content="This is a test description." />
            </head>
            <body>
                <h1 class="entry-title">Fallback Title</h1>
                <div class="chapter-list">
                    <a href="https://crotpedia.net/baca/test-manga-chapter-10-bahasa-indonesia/">Chapter 10</a>
                    <a href="https://crotpedia.net/baca/test-manga-chapter-2-bahasa-indonesia/">Chapter 2</a>
                    <a href="https://crotpedia.net/baca/test-manga-chapter-1-bahasa-indonesia/">Chapter 1</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(sampleSeriesHtml, "https://crotpedia.net/baca/series/test-manga/")
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")
        val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content")

        assertEquals("Meta Title Manga", ogTitle)
        assertEquals("https://i0.wp.com/crotpedia.net/wp-content/uploads/cover.jpg", ogImage)
    }

    @Test
    fun testExtractCoverUrlWithExternalCdn() {
        val html = """
            <html><body>
                <div class="series-thumb"><img src="https://cdn.external.com/manga/poster.jpg" /></div>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/series/test/")
        val cover = parser.extractCoverUrl(doc)
        assertEquals("https://cdn.external.com/manga/poster.jpg", cover)
    }

    @Test
    fun testSeriesParserPreservesOriginalDomOrderAndFiltersUnwantedLinks() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Series - CrotPedia</title>
                <meta property="og:title" content="Test Series" />
            </head>
            <body>
                <div class="series-thumb"><img src="https://crotpedia.net/wp-content/uploads/poster.jpg" /></div>
                <div class="chapter-list">
                    <a href="https://crotpedia.net/baca/chapter-1/">Chapter 1November 7, 2026</a>
                    <a href="https://crotpedia.net/baca/chapter-2">Chapter 2</a>
                    <a href="https://crotpedia.net/baca/chapter-2/">Chapter 2 (duplicate trailing slash)</a>
                    <a href="https://crotpedia.net/baca/chapter-10/">Chapter 10</a>
                    <a href="https://crotpedia.net/baca/chapter-2.5/">Chapter 2.5</a>
                    <a href="https://crotpedia.net/series/other-series/">Request List</a>
                    <a href="https://crotpedia.net/tag/ahegao/">Ahegao</a>
                    <a href="https://crotpedia.net/category/big-breast/">Big Breast</a>
                    <a href="https://crotpedia.net/baca/request/">Request</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/series/test-series/")
        val series = parser.parseSeriesDoc("https://crotpedia.net/baca/series/test-series/", doc)

        assertEquals("Test Series", series.title)
        assertEquals("https://crotpedia.net/wp-content/uploads/poster.jpg", series.coverUrl)
        assertEquals(4, series.chapters.size)
        // Original DOM order preserved
        assertEquals("Chapter 1", series.chapters[0].title)
        assertEquals("Chapter 2", series.chapters[1].title)
        assertEquals("Chapter 10", series.chapters[2].title)
        assertEquals("Chapter 2.5", series.chapters[3].title)
    }

    @Test
    fun testDuplicateChapterTitlesPreservedAsSeparateEntries() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Series - CrotPedia</title>
            </head>
            <body>
                <div class="chapter-list">
                    <a href="https://crotpedia.net/baca/chapter-1-v1/">Chapter 1</a>
                    <a href="https://crotpedia.net/baca/chapter-1-v2/">Chapter 1</a>
                    <a href="https://crotpedia.net/baca/chapter-2/">Chapter 2</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/series/test/")
        val series = parser.parseSeriesDoc("https://crotpedia.net/baca/series/test/", doc)

        assertEquals(3, series.chapters.size)
        assertEquals("Chapter 1", series.chapters[0].title)
        assertEquals("https://crotpedia.net/baca/chapter-1-v1", series.chapters[0].chapterUrl)
        assertEquals("Chapter 1", series.chapters[1].title)
        assertEquals("https://crotpedia.net/baca/chapter-1-v2", series.chapters[1].chapterUrl)
    }

    @Test
    fun testCoverExtractionPrioritizesSeriesThumbOverOgImagePromoBanner() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:image" content="https://crotpedia.net/wp-content/uploads/promo-banner.jpg" />
            </head>
            <body>
                <div class="series-thumb">
                    <img src="https://crotpedia.net/wp-content/uploads/real-manga-cover.jpg" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/series/test/")
        val coverUrl = parser.extractCoverUrl(doc)

        assertEquals("https://crotpedia.net/wp-content/uploads/real-manga-cover.jpg", coverUrl)
    }
}
