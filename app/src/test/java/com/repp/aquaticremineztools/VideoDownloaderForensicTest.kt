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
import org.junit.Assert.assertNotEquals
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
            assertNotEquals(dlPageUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test2_validHlsResolution() {
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
            assertEquals("application/x-mpegurl", resolved.mimeType)
        }
    }

    @Test
    fun test3_relativeMediaUrlResolution() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val expectedAbsolute = server.url("/media/stream.mp4").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=relative" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/media/stream.mp4"))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(expectedAbsolute, resolved.mediaUrl)
        }
    }

    @Test
    fun test4_htmlContainingSupportedVideoSource() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val directVideoUrl = server.url("/direct-video.mp4").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<html><body><video src="$directVideoUrl"></video></body></html>"""))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(directVideoUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test5_htmlContainingSupportedIframeSource() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val iframeMediaUrl = server.url("/embed/stream.mp4").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<html><body><iframe src="$iframeMediaUrl"></iframe></body></html>"""))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(iframeMediaUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test6_jsonContainingSupportedMediaUrl() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()
            val jsonMediaUrl = server.url("/stream/file.mp4").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=api" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setBody("""{"status":"success","url":"$jsonMediaUrl"}""").setHeader("Content-Type", "application/json"))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val resolved = parser.resolveDirectMediaSource(source)
            assertEquals(jsonMediaUrl, resolved.mediaUrl)
        }
    }

    @Test
    fun test7_htmlWithNoMediaUrl_MustFailWithException() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=html" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setBody("<html><body>AdBlock Error</body></html>").setHeader("Content-Type", "text/html"))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val exception = assertThrows(IOException::class.java) {
                runBlocking { parser.resolveDirectMediaSource(source) }
            }
            assertTrue(exception.message?.contains("[MEDIA_URL_NOT_FOUND]") == true)
        }
    }

    @Test
    fun test8_emptyGoResponse_MustFailWithException() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("""<a href="?go=empty" data-hcu-go>Download</a>"""))
            server.enqueue(MockResponse().setBody(""))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val exception = assertThrows(IOException::class.java) {
                runBlocking { parser.resolveDirectMediaSource(source) }
            }
            assertTrue(exception.message?.contains("[MEDIA_URL_NOT_FOUND]") == true)
        }
    }

    @Test
    fun test9_http403ForbiddenResponse_MustFailWithException() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setResponseCode(403))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            val exception = assertThrows(IOException::class.java) {
                runBlocking { parser.resolveDirectMediaSource(source) }
            }
            assertTrue(exception.message?.contains("[HTTP_FORBIDDEN]") == true)
        }
    }

    @Test
    fun test10_downloadPageUrlMustNeverBeReturnedAsMediaUrl() {
        runBlocking {
            val refererUrl = server.url("/refer/").toString()
            val dlPageUrl = server.url("/dl-page/hitori-tsuma").toString()

            server.enqueue(MockResponse().setBody("OK"))
            server.enqueue(MockResponse().setBody("<html><body>No video elements here</body></html>"))

            val source = createTestVideoSource(dlPageUrl, refererUrl)
            assertThrows(IOException::class.java) {
                runBlocking { parser.resolveDirectMediaSource(source) }
            }
        }
    }

    @Test
    fun test11_extensionlessMediaUrlValidation() {
        val dlPageUrl = "https://hentaicop.com/unduh/ep1"
        val extensionlessCdnUrl = "https://cdn.hentaicop.com/stream/v12345?token=abc"

        assertTrue(HentaiCopParser.isValidMediaUrl(extensionlessCdnUrl, dlPageUrl))
    }

    @Test
    fun test12_invalidUrlSchemeRejected() {
        val dlPageUrl = "https://hentaicop.com/unduh/ep1"
        val jsUrl = "javascript:alert(1)"
        val fileUrl = "file:///etc/passwd"

        assertTrue(!HentaiCopParser.isValidMediaUrl(jsUrl, dlPageUrl))
        assertTrue(!HentaiCopParser.isValidMediaUrl(fileUrl, dlPageUrl))
    }

    @Test
    fun test13_unrelatedAdvertisementUrlRejected() {
        val dlPageUrl = "https://hentaicop.com/unduh/ep1"
        val adUrl = "https://popunder.adsterra.com/click?id=123"
        val trackingUrl = "https://www.google-analytics.com/collect"

        assertTrue(!HentaiCopParser.isValidMediaUrl(adUrl, dlPageUrl))
        assertTrue(!HentaiCopParser.isValidMediaUrl(trackingUrl, dlPageUrl))
    }
}
