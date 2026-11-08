package com.repp.aquaticremineztools

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.data.manga.model.ChapterAnalysisStatus
import com.repp.aquaticremineztools.data.manga.model.MangaChapter
import com.repp.aquaticremineztools.data.manga.model.MangaSeries
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.presentation.manga.AnalyzeState
import com.repp.aquaticremineztools.presentation.manga.MangaChapterAdapter
import com.repp.aquaticremineztools.presentation.manga.MangaViewModel
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [33])
class MangaPhase1Test {

    private lateinit var app: Application
    private lateinit var parser: CrotPediaParser

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        parser = CrotPediaParser()
    }

    @Test
    fun testSeriesUrlValidation() {
        assertTrue(CrotPediaParser.isAllowedUrl("https://crotpedia.net/baca/series/test/"))
        assertTrue(CrotPediaParser.isAllowedUrl("http://crotpedia.net/baca/chapter-1/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://evil.com/crotpedia.net/"))
        assertFalse(CrotPediaParser.isAllowedUrl("invalid-url"))
    }

    @Test
    fun testInvalidUrlInViewModelEmitsError() {
        val viewModel = MangaViewModel(app)
        viewModel.analyzeUrl("https://invalid-domain.com/series/test")

        val state = viewModel.analyzeState.value
        assertTrue(state is AnalyzeState.Error)
        val errorState = state as AnalyzeState.Error
        assertTrue(errorState.isInvalidUrl)
        assertNull(viewModel.currentSeries)
    }

    @Test
    fun testDuplicateChapterTitlesPreservedAsSeparateEntries() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>Duplicate Series - CrotPedia</title></head>
            <body>
                <div class="chapter-list">
                    <a href="https://crotpedia.net/baca/ch1-v1/">Chapter 1</a>
                    <a href="https://crotpedia.net/baca/ch1-v2/">Chapter 1</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/series/test/")
        val series = parser.parseSeriesDoc("https://crotpedia.net/baca/series/test/", doc)

        assertEquals(2, series.chapters.size)
        assertEquals("Chapter 1", series.chapters[0].title)
        assertEquals("https://crotpedia.net/baca/ch1-v1", series.chapters[0].chapterUrl)
        assertEquals("Chapter 1", series.chapters[1].title)
        assertEquals("https://crotpedia.net/baca/ch1-v2", series.chapters[1].chapterUrl)
        assertFalse(series.chapters[0].id == series.chapters[1].id)
    }

    @Test
    fun testOriginalChapterOrderPreservedInDOMOrder() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>DOM Order Series - CrotPedia</title></head>
            <body>
                <div class="chapter-list">
                    <a href="https://crotpedia.net/baca/ch-100/">Chapter 100</a>
                    <a href="https://crotpedia.net/baca/ch-99/">Chapter 99</a>
                    <a href="https://crotpedia.net/baca/ch-1/">Chapter 1</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/series/test/")
        val series = parser.parseSeriesDoc("https://crotpedia.net/baca/series/test/", doc)

        assertEquals(3, series.chapters.size)
        assertEquals("Chapter 100", series.chapters[0].title)
        assertEquals("Chapter 99", series.chapters[1].title)
        assertEquals("Chapter 1", series.chapters[2].title)
    }

    @Test
    fun testReaderImageFilteringExcludesAdsAndLogos() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>Chapter Page</title></head>
            <body>
                <div id="readerarea">
                    <img src="https://crotpedia.net/logo.png" />
                    <img src="https://i0.wp.com/crotpedia.net/wp-content/uploads/01.jpg" />
                    <div class="ad-container">
                        <img src="https://crotpedia.net/banner.gif" />
                    </div>
                    <img data-src="https://cdn.external.com/manga/02.webp" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/chapter-1/")
        val method = CrotPediaParser::class.java.getDeclaredMethod("extractPagesFromChapterDoc", org.jsoup.nodes.Document::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val pages = method.invoke(parser, doc) as List<com.repp.aquaticremineztools.data.manga.model.MangaPage>

        assertEquals(2, pages.size)
        assertEquals("https://i0.wp.com/crotpedia.net/wp-content/uploads/01.jpg", pages[0].imageUrl)
        assertEquals("https://cdn.external.com/manga/02.webp", pages[1].imageUrl)
    }

    @Test
    fun testCoverExtractionFromExternalCdn() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:image" content="https://images.cdn.org/manga/cover.jpg" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://crotpedia.net/baca/series/test/")
        val coverUrl = parser.extractCoverUrl(doc)
        assertEquals("https://images.cdn.org/manga/cover.jpg", coverUrl)
    }

    @Test
    fun testReanalysisConfirmationTriggeredForSameUrl() {
        val viewModel = MangaViewModel(app)
        val testUrl = "https://crotpedia.net/baca/series/same-test/"

        val seriesField = MangaViewModel::class.java.getDeclaredField("currentSeries")
        seriesField.isAccessible = true
        val sampleSeries = MangaSeries("Title", "cover.jpg", null, testUrl, listOf(
            MangaChapter(title = "Ch 1", chapterUrl = "$testUrl/ch1/", pageCount = 10, orderIndex = 1, status = ChapterAnalysisStatus.SUCCESS)
        ))
        seriesField.set(viewModel, sampleSeries)

        val lastUrlField = MangaViewModel::class.java.getDeclaredField("lastAnalyzedUrl")
        lastUrlField.isAccessible = true
        val canonicalMethod = MangaViewModel::class.java.getDeclaredMethod("canonicalUrl", String::class.java)
        canonicalMethod.isAccessible = true
        val canonical = canonicalMethod.invoke(viewModel, testUrl) as String
        lastUrlField.set(viewModel, canonical)

        viewModel.analyzeUrl(testUrl)
        val state = viewModel.analyzeState.value
        assertTrue(state is AnalyzeState.ShowReanalysisConfirmation)
        assertEquals(testUrl, (state as AnalyzeState.ShowReanalysisConfirmation).url)
    }

    @Test
    fun testCancelAnalysisClearsState() {
        val viewModel = MangaViewModel(app)
        val seriesField = MangaViewModel::class.java.getDeclaredField("currentSeries")
        seriesField.isAccessible = true
        seriesField.set(viewModel, MangaSeries("Title", "cover.jpg", null, "url", emptyList()))

        viewModel.cancelAnalysis()

        assertNull(viewModel.currentSeries)
        assertTrue(viewModel.analyzeState.value is AnalyzeState.Idle)
    }

    @Test
    fun testChapterAdapterBindingForFailedStatus() {
        val chFailed = MangaChapter(
            id = "ch-1",
            title = "Chapter 1",
            chapterUrl = "https://crotpedia.net/baca/ch1/",
            pageCount = -1,
            orderIndex = 1,
            status = ChapterAnalysisStatus.FAILED
        )

        val adapter = MangaChapterAdapter(
            onDownloadClick = {},
            onRetryClick = {}
        )
        adapter.setChapters(listOf(chFailed))

        assertEquals(1, adapter.itemCount)
    }
}
