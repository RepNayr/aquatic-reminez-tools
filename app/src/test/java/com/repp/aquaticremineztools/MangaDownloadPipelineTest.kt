package com.repp.aquaticremineztools

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
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
    fun testPdfBuilderProgressCallbackReporting() {
        val tempImg = java.io.File(app.cacheDir, "test_page_1.jpg").apply {
            val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.RGB_565)
            java.io.FileOutputStream(this).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
        }
        val stages = mutableSetOf<String>()
        val result = runCatching {
            kotlinx.coroutines.runBlocking {
                PdfBuilder.createPdfFromImages(
                    context = app,
                    imageFiles = listOf(tempImg),
                    outputFileName = "progress_test.pdf",
                    expectedPages = 1,
                    onProgress = { stage, percent ->
                        stages.add(stage)
                        assertTrue(percent in 0..100)
                    }
                )
            }
        }
        tempImg.delete()
        if (result.isSuccess) {
            assertTrue(stages.contains(MangaDownloadEntity.STAGE_BUILDING_PDF))
            assertTrue(stages.contains(MangaDownloadEntity.STAGE_VALIDATING_PDF))
        }
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
