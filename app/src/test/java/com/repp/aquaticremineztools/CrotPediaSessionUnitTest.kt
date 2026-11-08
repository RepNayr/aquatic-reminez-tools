package com.repp.aquaticremineztools

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.presentation.manga.MangaViewModel
import com.repp.aquaticremineztools.presentation.manga.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = [33])
class CrotPediaSessionUnitTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        CrotPediaSessionStore.clearSession(app)
    }

    @Test
    fun testAllowedCookieHostValidation() {
        assertTrue(CrotPediaSessionStore.isAllowedCookieHost("crotpedia.net"))
        assertTrue(CrotPediaSessionStore.isAllowedCookieHost("www.crotpedia.net"))
        assertTrue(CrotPediaSessionStore.isAllowedCookieHost("sub.crotpedia.net"))

        assertFalse(CrotPediaSessionStore.isAllowedCookieHost("evilcrotpedia.net"))
        assertFalse(CrotPediaSessionStore.isAllowedCookieHost("crotpedia.net.evil.com"))
        assertFalse(CrotPediaSessionStore.isAllowedCookieHost(""))
        assertFalse(CrotPediaSessionStore.isAllowedCookieHost(null))
    }

    @Test
    fun testSessionStoreSaveAndClear() {
        assertFalse(CrotPediaSessionStore.isLoggedIn(app))

        CrotPediaSessionStore.saveSession(app, "wordpress_logged_in_test=abc123xyz")
        assertTrue(CrotPediaSessionStore.isLoggedIn(app))
        assertEquals("wordpress_logged_in_test=abc123xyz", CrotPediaSessionStore.getSavedCookies(app))

        CrotPediaSessionStore.clearSession(app)
        assertFalse(CrotPediaSessionStore.isLoggedIn(app))
        assertEquals(null, CrotPediaSessionStore.getSavedCookies(app))
    }

    @Test
    fun testViewModelInitialSessionStateIsNotLoggedInWhenNoSession() {
        val viewModel = MangaViewModel(app)
        val state = viewModel.sessionState.value
        assertTrue(state is SessionState.NotLoggedIn)
    }
}
