package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrotPediaParserUnitTest {

    @Test
    fun testAllowedUrl() {
        assertTrue(CrotPediaParser.isAllowedUrl("https://crotpedia.net/baca/series/test-series/"))
        assertTrue(CrotPediaParser.isAllowedUrl("http://www.crotpedia.net/baca/chapter-1/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://otherdomain.com/series/test/"))
        assertFalse(CrotPediaParser.isAllowedUrl("invalid-url"))
    }

    @Test
    fun testParseSeriesHtmlFixture() {
        val sampleSeriesHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Manga Series - CrotPedia</title>
                <meta property="og:title" content="Test Manga Series" />
                <meta property="og:image" content="https://crotpedia.net/wp-content/uploads/cover.jpg" />
            </head>
            <body>
                <h1 class="entry-title">Test Manga Series</h1>
                <div class="series-thumb">
                    <img src="https://crotpedia.net/wp-content/uploads/cover.jpg" />
                </div>
                <div class="chapter-list">
                    <a href="https://crotpedia.net/baca/test-manga-chapter-1-bahasa-indonesia/">Chapter 1</a>
                    <a href="https://crotpedia.net/baca/test-manga-chapter-2-bahasa-indonesia/">Chapter 2</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(sampleSeriesHtml, "https://crotpedia.net/baca/series/test-manga/")
        val title = doc.selectFirst(".entry-title")?.text()
        val chapterLinks = doc.select(".chapter-list a")

        assertEquals("Test Manga Series", title)
        assertEquals(2, chapterLinks.size)
        assertEquals("Chapter 1", chapterLinks[0].text())
        assertEquals("Chapter 2", chapterLinks[1].text())
    }

    @Test
    fun testParseChapterHtmlFixture() {
        val sampleChapterHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Manga Chapter 1 Bahasa Indonesia - CrotPedia</title>
            </head>
            <body>
                <h1 class="entry-title">Test Manga Chapter 1 Bahasa Indonesia</h1>
                <div id="readerarea">
                    <img src="https://crotpedia.net/wp-content/uploads/2023/01/01.jpg" />
                    <img src="https://crotpedia.net/wp-content/uploads/2023/01/02.jpg" />
                    <img src="https://crotpedia.net/wp-content/uploads/2023/01/03.jpg" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(sampleChapterHtml, "https://crotpedia.net/baca/test-manga-chapter-1-bahasa-indonesia/")
        val images = doc.select("#readerarea img")

        assertEquals(3, images.size)
        assertEquals("https://crotpedia.net/wp-content/uploads/2023/01/01.jpg", images[0].attr("src"))
    }
}
