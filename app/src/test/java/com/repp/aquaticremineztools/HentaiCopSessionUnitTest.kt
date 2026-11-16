package com.repp.aquaticremineztools

import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.core.security.HentaiCopSessionStore
import com.repp.aquaticremineztools.data.manga.source.CrotPediaCookieJar
import com.repp.aquaticremineztools.data.video.network.HentaiCopCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HentaiCopSessionUnitTest {

    private lateinit var crotPediaCookieJar: CrotPediaCookieJar
    private lateinit var hentaiCopCookieJar: HentaiCopCookieJar

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        CrotPediaSessionStore.clearSession(context)
        HentaiCopSessionStore.clearSession(context)

        crotPediaCookieJar = CrotPediaCookieJar(context)
        hentaiCopCookieJar = HentaiCopCookieJar(context)
    }

    @Test
    fun testDomainIsolation_HentaiCopHostValidation() {
        assertTrue(HentaiCopSessionStore.isAllowedCookieHost("hentaicop.com"))
        assertTrue(HentaiCopSessionStore.isAllowedCookieHost("cdn.hentaicop.com"))
        assertFalse(HentaiCopSessionStore.isAllowedCookieHost("crotpedia.net"))
        assertFalse(HentaiCopSessionStore.isAllowedCookieHost("google.com"))
    }

    @Test
    fun testDomainIsolation_CrotPediaHostValidation() {
        assertTrue(CrotPediaSessionStore.isAllowedCookieHost("crotpedia.net"))
        assertTrue(CrotPediaSessionStore.isAllowedCookieHost("cdn.crotpedia.net"))
        assertFalse(CrotPediaSessionStore.isAllowedCookieHost("hentaicop.com"))
        assertFalse(CrotPediaSessionStore.isAllowedCookieHost("google.com"))
    }

    @Test
    fun testCookieJarIsolation_HentaiCopDoesNotAcceptCrotPedia() {
        val crotPediaUrl = "https://crotpedia.net/login".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session_id")
            .value("crotpedia_secret_token")
            .domain("crotpedia.net")
            .path("/")
            .build()

        hentaiCopCookieJar.saveFromResponse(crotPediaUrl, listOf(cookie))

        val loadedForCrotPedia = hentaiCopCookieJar.loadForRequest(crotPediaUrl)
        assertTrue(loadedForCrotPedia.isEmpty())

        val hentaiCopUrl = "https://hentaicop.com/video".toHttpUrl()
        val loadedForHentaiCop = hentaiCopCookieJar.loadForRequest(hentaiCopUrl)
        assertTrue(loadedForHentaiCop.isEmpty())
    }

    @Test
    fun testCookieJarIsolation_CrotPediaDoesNotAcceptHentaiCop() {
        val hentaiCopUrl = "https://hentaicop.com/login".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session_id")
            .value("hentaicop_secret_token")
            .domain("hentaicop.com")
            .path("/")
            .build()

        crotPediaCookieJar.saveFromResponse(hentaiCopUrl, listOf(cookie))

        val loadedForHentaiCop = crotPediaCookieJar.loadForRequest(hentaiCopUrl)
        assertTrue(loadedForHentaiCop.isEmpty())

        val crotPediaUrl = "https://crotpedia.net/manga".toHttpUrl()
        val loadedForCrotPedia = crotPediaCookieJar.loadForRequest(crotPediaUrl)
        assertTrue(loadedForCrotPedia.isEmpty())
    }

    @Test
    fun testHentaiCopCookieStorageAndRetrieval() {
        val hentaiCopUrl = "https://hentaicop.com/watch".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("hcu_session")
            .value("valid_session_123")
            .domain("hentaicop.com")
            .path("/")
            .build()

        hentaiCopCookieJar.saveFromResponse(hentaiCopUrl, listOf(cookie))

        val loaded = hentaiCopCookieJar.loadForRequest(hentaiCopUrl)
        assertEquals(1, loaded.size)
        assertEquals("hcu_session", loaded[0].name)
        assertEquals("valid_session_123", loaded[0].value)
    }

    @Test
    fun testClearingHentaiCopSessionDoesNotClearCrotPedia() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        CrotPediaSessionStore.saveSession(context, "crot_session=123")
        HentaiCopSessionStore.saveSession(context, "hcu_session=abc")

        HentaiCopSessionStore.clearSession(context)

        assertFalse(HentaiCopSessionStore.isLoggedIn(context))
        assertTrue(CrotPediaSessionStore.isLoggedIn(context))
        assertEquals("crot_session=123", CrotPediaSessionStore.getSavedCookies(context))
    }
}
