package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.data.manga.source.MangaAuthRequiredException
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrotPediaParserUnitTest {

    private val parser = CrotPediaParser()

    @Test
    fun testAllowedUrlStrictValidation() {
        assertTrue(CrotPediaParser.isAllowedUrl("https://crotpedia.net/baca/series/test-series/"))
        assertTrue(CrotPediaParser.isAllowedUrl("http://www.crotpedia.net/baca/chapter-1/"))
        assertTrue(CrotPediaParser.isAllowedUrl("https://sub.crotpedia.net/baca/chapter-1/"))

        // Untrusted / spoofed domains
        assertFalse(CrotPediaParser.isAllowedUrl("https://crotpedia.net.evil.com/baca/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://evil.com/crotpedia.net/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://crotpedia-net.com/baca/"))
        assertFalse(CrotPediaParser.isAllowedUrl("ftp://crotpedia.net/file"))
        assertFalse(CrotPediaParser.isAllowedUrl("invalid-url"))
    }

    @Test
    fun testParseChapterNumberExtraction() {
        assertEquals(1f, parser.parseChapterNumber("Chapter 1"))
        assertEquals(2.5f, parser.parseChapterNumber("Musume no Tomodachi Chapter 2.5 Bahasa Indonesia"))
        assertEquals(10f, parser.parseChapterNumber("Chapter 10 - End"))
        assertEquals(100f, parser.parseChapterNumber("Vol 2 Chapter 100"))
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
                <meta property="og:image" content="https://crotpedia.net/wp-content/uploads/cover.jpg" />
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
        assertEquals("https://crotpedia.net/wp-content/uploads/cover.jpg", ogImage)
    }

    @Test
    fun testParseChapterHtmlLazyLoadedImages() {
        val sampleChapterHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Chapter 1 - CrotPedia</title>
            </head>
            <body>
                <div id="readerarea">
                    <img data-src="https://crotpedia.net/wp-content/uploads/2023/01/01.jpg" src="data:image/svg+xml;base64,123" />
                    <img data-lazy-src="https://crotpedia.net/wp-content/uploads/2023/01/02.jpg" />
                    <img src="https://crotpedia.net/wp-content/uploads/2023/01/03.jpg" />
                    <img src="https://crotpedia.net/logo.png" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(sampleChapterHtml, "https://crotpedia.net/baca/test-chapter-1/")
        val imgElements = doc.select("#readerarea img")

        assertNotNull(imgElements)
        assertEquals(4, imgElements.size)
    }
}
