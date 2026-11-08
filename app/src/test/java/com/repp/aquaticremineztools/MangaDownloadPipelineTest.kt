package com.repp.aquaticremineztools

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.data.manga.source.MangaAuthRequiredException
import com.repp.aquaticremineztools.domain.manga.PdfBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [33])
class MangaDownloadPipelineTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        CrotPediaSessionStore.clearSession(app)
    }

    @Test
    fun testPdfBuilderEmptyImagesThrowsException() {
        val result = runCatching {
            kotlinx.coroutines.runBlocking {
                PdfBuilder.createPdfFromImages(
                    context = app,
                    imageFiles = emptyList(),
                    outputFileName = "test.pdf",
                    expectedPages = 0
                )
            }
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testMangaAuthRequiredExceptionClassExists() {
        val ex = MangaAuthRequiredException("Session expired")
        assertEquals("Session expired", ex.message)
    }

    @Test
    fun testCrotPediaSessionStoreFailClosedBehavior() {
        assertTrue(CrotPediaSessionStore.isAllowedCookieHost("crotpedia.net"))
        assertFalse(CrotPediaSessionStore.isAllowedCookieHost("evil.com"))
    }
}
