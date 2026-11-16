package com.repp.aquaticremineztools

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.data.local.video.VideoDatabase
import com.repp.aquaticremineztools.data.video.model.VideoQuality
import com.repp.aquaticremineztools.data.video.model.VideoSource
import com.repp.aquaticremineztools.data.video.source.HentaiCopParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class VideoDownloaderForensicTest {

    private lateinit var server: MockWebServer
    private lateinit var parser: HentaiCopParser
    private lateinit var client: OkHttpClient
    private lateinit var db: VideoDatabase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().build()
        parser = HentaiCopParser(ApplicationProvider.getApplicationContext(), client)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VideoDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    private fun createTestVideoSource(downloadPageUrl: String, refererUrl: String): VideoSource {
        return VideoSource(
            serverId = "hentaicop_test_720p",
            serverName = "HentaiCop",
            quality = VideoQuality(label = "720p", height = 720, fileSizeBytes = 1000L),
            downloadPageUrl = downloadPageUrl,
            refererHeader = refererUrl
        )
    }

    @Test
    fun test1_validDirectMp4Resolution() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val mediaUrl = server.url("/video.mp4").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=1" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", mediaUrl))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)

            assertEquals(mediaUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test2_http302WithValidLocation() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val targetUrl = server.url("/final/stream.mp4").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=stream" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", targetUrl))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)

            assertEquals(targetUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test3_http200ReturningHtmlFallback() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=html" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setBody("<html><body>AdBlock Error</body></html>").setHeader("Content-Type", "text/html"))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(dlPageUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test4_http403ResponseFallback() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=forbidden" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setResponseCode(403))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(dlPageUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test5_emptyResponseBodyFallback() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody(""))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(dlPageUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test6_invalidOrUnsupportedContentTypeFallback() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=json" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setBody("""{"error":"unauthorized"}""").setHeader("Content-Type", "application/json"))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(dlPageUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test7_hlsManifestResponse() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val hlsUrl = server.url("/playlist.m3u8").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=hls" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", hlsUrl))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertTrue(resolved.mediaUrl.contains(".m3u8"))
        }
    }

    @Test
    fun test8_networkConnectionFailure() {
        server.shutdown() // Shutdown immediately

        val refererUrl = "http://localhost:1/refer"
        val dlPageUrl = "http://localhost:1/dl-page"
        val source = createTestVideoSource(dlPageUrl, refererUrl)
        assertThrows(IOException::class.java) {
            runBlocking {
                parser.resolveDirectMediaSource(source)
            }
        }
    }

    @Test
    fun test9_redirectChainWithValidFinalResponse() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val intermediateUrl = server.url("/redirect-intermediate").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=chain" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", intermediateUrl))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(intermediateUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test10_missingGoLinkFallback() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("<html>No go link here</html>"))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(dlPageUrl, resolved.mediaUrl)
        }
    }
}
