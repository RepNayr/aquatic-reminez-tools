package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.video.source.HentaiCopParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HentaiCopParserUnitTest {

    private lateinit var parser: HentaiCopParser

    @Before
    fun setUp() {
        parser = HentaiCopParser()
    }

    @Test
    fun testIsAllowedSeriesUrl() {
        assertTrue(HentaiCopParser.isAllowedSeriesUrl("https://hentaicop.com/series/hitori-tsuma/"))
        assertTrue(HentaiCopParser.isAllowedSeriesUrl("hentaicop.com/series/test/"))
        assertFalse(HentaiCopParser.isAllowedSeriesUrl("https://google.com/series/hitori-tsuma/"))
        assertFalse(HentaiCopParser.isAllowedSeriesUrl("https://hentaicop.com/hitori-tsuma-episode-1/"))
        assertFalse(HentaiCopParser.isAllowedSeriesUrl("invalid-url"))
    }

    @Test
    fun testCanonicalizeUrl() {
        assertEquals("https://hentaicop.com/series/test/", HentaiCopParser.canonicalizeUrl("hentaicop.com/series/test/"))
        assertEquals("https://hentaicop.com/series/test/", HentaiCopParser.canonicalizeUrl("https://hentaicop.com/series/test/"))
    }
}
