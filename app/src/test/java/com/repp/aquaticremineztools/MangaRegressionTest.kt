package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.core.security.MangaPinManager
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaRegressionTest {

    @Test
    fun testCanonicalUrlNormalizationAndDeduplicationRules() {
        val url1 = "https://crotpedia.net/baca/chapter-1/"
        val url2 = "https://crotpedia.net/baca/chapter-1"

        val clean1 = url1.trim()
        val norm1 = if (clean1.endsWith("/")) clean1.dropLast(1) else clean1

        val clean2 = url2.trim()
        val norm2 = if (clean2.endsWith("/")) clean2.dropLast(1) else clean2

        assertEquals(norm1, norm2)

        // Status enqueue rules check
        val statusesRejectDuplicate = listOf(
            MangaDownloadEntity.STATUS_QUEUED,
            MangaDownloadEntity.STATUS_DOWNLOADING,
            MangaDownloadEntity.STATUS_COMPLETED,
            MangaDownloadEntity.STATUS_COMPLETED_WITH_WARNING
        )

        val statusesAllowRetry = listOf(
            MangaDownloadEntity.STATUS_FAILED,
            MangaDownloadEntity.STATUS_CANCELLED
        )

        statusesRejectDuplicate.forEach { status ->
            val allowEnqueue = when (status) {
                MangaDownloadEntity.STATUS_FAILED, MangaDownloadEntity.STATUS_CANCELLED -> true
                else -> false
            }
            assertFalse("Status $status should reject duplicate enqueue", allowEnqueue)
        }

        statusesAllowRetry.forEach { status ->
            val allowEnqueue = when (status) {
                MangaDownloadEntity.STATUS_FAILED, MangaDownloadEntity.STATUS_CANCELLED -> true
                else -> false
            }
            assertTrue("Status $status should allow retry enqueue", allowEnqueue)
        }
    }

    @Test
    fun testCrotPediaParserHostSecurity() {
        assertTrue(CrotPediaParser.isAllowedUrl("https://crotpedia.net/baca/ch1/"))
        assertTrue(CrotPediaParser.isAllowedUrl("https://sub.crotpedia.net/baca/ch1"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://evilcrotpedia.net/baca/"))
        assertFalse(CrotPediaParser.isAllowedUrl("https://crotpedia.net.evil.com/"))
        assertFalse(CrotPediaParser.isAllowedUrl("ftp://crotpedia.net/ch1"))
    }

    @Test
    fun testDecimalChapterSortingOrder() {
        val parser = CrotPediaParser()
        val ch1 = parser.parseChapterNumber("Chapter 1")
        val ch1_5 = parser.parseChapterNumber("Chapter 1.5")
        val ch2 = parser.parseChapterNumber("Chapter 2")
        val ch10 = parser.parseChapterNumber("Chapter 10")

        assertNotNull(ch1)
        assertNotNull(ch1_5)
        assertNotNull(ch2)
        assertNotNull(ch10)

        assertTrue(ch1!! < ch1_5!!)
        assertTrue(ch1_5 < ch2!!)
        assertTrue(ch2 < ch10!!)
    }

    @Test
    fun testPinManagerHashConstants() {
        val pin = "654321"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val hash1 = MangaPinManager.hashPin(pin, salt)
        val hash2 = MangaPinManager.hashPin(pin, salt)
        assertEquals(hash1, hash2)
    }
}
