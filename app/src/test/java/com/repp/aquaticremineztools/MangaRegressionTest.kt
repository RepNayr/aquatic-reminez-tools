package com.repp.aquaticremineztools

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.core.security.MangaPinManager
import com.repp.aquaticremineztools.data.local.manga.MangaDatabase
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadDao
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.manga.model.MangaChapter
import com.repp.aquaticremineztools.data.manga.repository.MangaRepository
import com.repp.aquaticremineztools.data.manga.source.CrotPediaParser
import com.repp.aquaticremineztools.domain.manga.PdfBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MangaRegressionTest {

    private lateinit var context: Context
    private lateinit var db: MangaDatabase
    private lateinit var dao: MangaDownloadDao
    private lateinit var repository: MangaRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MangaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.mangaDownloadDao()
        repository = MangaRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUrlNormalizationAndDeduplicationInDatabase() = runBlocking {
        val rawUrlWithSlash = "https://crotpedia.net/baca/chapter-1/"
        val rawUrlNoSlash = "https://crotpedia.net/baca/chapter-1"

        val chapterWithSlash = MangaChapter(title = "Chapter 1", chapterUrl = rawUrlWithSlash, pageCount = 20, orderIndex = 1)
        val chapterNoSlash = MangaChapter(title = "Chapter 1", chapterUrl = rawUrlNoSlash, pageCount = 20, orderIndex = 1)

        // 1. Enqueue first URL with trailing slash
        val added1 = repository.enqueueChapter("Test Series", chapterWithSlash)
        assertTrue(added1)

        // 2. Attempt enqueue duplicate URL without trailing slash -> should be rejected
        val added2 = repository.enqueueChapter("Test Series", chapterNoSlash)
        assertFalse(added2)

        // Verify only 1 record in DB
        val item = dao.getByUrl("https://crotpedia.net/baca/chapter-1", "https://crotpedia.net/baca/chapter-1", "https://crotpedia.net/baca/chapter-1/")
        assertNotNull(item)
        assertEquals(MangaDownloadEntity.STATUS_QUEUED, item?.status)

        val countActive = dao.countExistingActiveOrCompleted("https://crotpedia.net/baca/chapter-1", "https://crotpedia.net/baca/chapter-1", "https://crotpedia.net/baca/chapter-1/")
        assertEquals(1, countActive)
    }

    @Test
    fun testEnqueueStatusRulesInDatabase() = runBlocking {
        val ch = MangaChapter(title = "Chapter 2", chapterUrl = "https://crotpedia.net/baca/chapter-2", pageCount = 15, orderIndex = 2)

        // Initial enqueue (status QUEUED)
        assertTrue(repository.enqueueChapter("Series A", ch))
        val item = dao.getByUrl("https://crotpedia.net/baca/chapter-2", "https://crotpedia.net/baca/chapter-2", "https://crotpedia.net/baca/chapter-2/")!!

        // QUEUED rejects duplicate
        assertFalse(repository.enqueueChapter("Series A", ch))

        // DOWNLOADING rejects duplicate
        dao.claimQueuedItem(item.id)
        assertFalse(repository.enqueueChapter("Series A", ch))

        // CANCELLED allows retry
        dao.cancelDownload(item.id)
        val cancelledItem = dao.getById(item.id)!!
        assertEquals(MangaDownloadEntity.STATUS_CANCELLED, cancelledItem.status)

        // Enqueue again after CANCELLED succeeds
        assertTrue(repository.enqueueChapter("Series A", ch))
        val retriedItem = dao.getById(item.id)!!
        assertEquals(MangaDownloadEntity.STATUS_QUEUED, retriedItem.status)
        assertNull(retriedItem.pdfPath)

        // Move to COMPLETED
        dao.claimQueuedItem(retriedItem.id)
        dao.markCompletedIfDownloading(
            id = retriedItem.id,
            finalStatus = MangaDownloadEntity.STATUS_COMPLETED,
            totalPages = 15,
            downloadedPages = 15,
            bytes = 1000L,
            totalBytes = 1000L,
            pdfPath = "file://test.pdf",
            warningMessage = null
        )

        // COMPLETED rejects duplicate enqueue
        assertFalse(repository.enqueueChapter("Series A", ch))

        // Completed history preserved
        val completedItem = dao.getById(retriedItem.id)!!
        assertEquals(MangaDownloadEntity.STATUS_COMPLETED, completedItem.status)
        assertEquals("file://test.pdf", completedItem.pdfPath)
    }

    @Test
    fun testTotalPagesPersistenceAndProgressUpdates() = runBlocking {
        val entity = MangaDownloadEntity(
            seriesTitle = "Series B",
            chapterTitle = "Chapter 3",
            chapterUrl = "https://crotpedia.net/baca/chapter-3",
            outputFileName = "ch3.pdf",
            totalPages = -1,
            status = MangaDownloadEntity.STATUS_QUEUED
        )
        val id = dao.insert(entity)

        dao.claimQueuedItem(id)

        // Worker discovers 25 total pages
        val totalPages = 25
        val rows1 = dao.updateProgressIfDownloading(id, totalPages = totalPages, downloadedPages = 0, bytes = 0L, totalBytes = 0L, speed = 0L)
        assertEquals(1, rows1)

        val updated1 = dao.getById(id)!!
        assertEquals(25, updated1.totalPages)
        assertEquals(0, updated1.downloadedPages)

        // Progress update for page 10
        val rows2 = dao.updateProgressIfDownloading(id, totalPages = totalPages, downloadedPages = 10, bytes = 5000L, totalBytes = 12500L, speed = 1000L)
        assertEquals(1, rows2)

        val updated2 = dao.getById(id)!!
        assertEquals(25, updated2.totalPages)
        assertEquals(10, updated2.downloadedPages)

        // Completion
        val rows3 = dao.markCompletedIfDownloading(id, MangaDownloadEntity.STATUS_COMPLETED, totalPages = totalPages, downloadedPages = totalPages, bytes = 12500L, totalBytes = 12500L, pdfPath = "file://ch3.pdf", warningMessage = null)
        assertEquals(1, rows3)

        val completed = dao.getById(id)!!
        assertEquals(MangaDownloadEntity.STATUS_COMPLETED, completed.status)
        assertEquals(25, completed.totalPages)
        assertEquals(25, completed.downloadedPages)
    }

    @Test
    fun testCancelledStateProtectionAgainstUpdates() = runBlocking {
        val entity = MangaDownloadEntity(
            seriesTitle = "Series C",
            chapterTitle = "Chapter 4",
            chapterUrl = "https://crotpedia.net/baca/chapter-4",
            outputFileName = "ch4.pdf",
            totalPages = 10,
            status = MangaDownloadEntity.STATUS_QUEUED
        )
        val id = dao.insert(entity)
        dao.claimQueuedItem(id)

        // User cancels
        dao.cancelDownload(id)

        // Progress update should affect 0 rows
        val progressRows = dao.updateProgressIfDownloading(id, totalPages = 10, downloadedPages = 5, bytes = 100L, totalBytes = 200L, speed = 10L)
        assertEquals(0, progressRows)

        // Completion update should affect 0 rows
        val completedRows = dao.markCompletedIfDownloading(id, MangaDownloadEntity.STATUS_COMPLETED, totalPages = 10, downloadedPages = 10, bytes = 200L, totalBytes = 200L, pdfPath = "file://ch4.pdf", warningMessage = null)
        assertEquals(0, completedRows)

        // Failed update should affect 0 rows
        val failedRows = dao.markFailedIfDownloading(id, "Error")
        assertEquals(0, failedRows)

        // State remains CANCELLED
        val current = dao.getById(id)!!
        assertEquals(MangaDownloadEntity.STATUS_CANCELLED, current.status)
    }

    @Test
    fun testHeartbeatRecoveryLegacyAndStuckItems() = runBlocking {
        val now = System.currentTimeMillis()
        val stuckTime = now - 600000L // 10 mins ago

        // 1. Legacy item with lastHeartbeatAt = 0 in DOWNLOADING
        val legacyId = dao.insert(MangaDownloadEntity(seriesTitle = "S", chapterTitle = "Ch 1", chapterUrl = "https://crotpedia.net/baca/legacy", outputFileName = "1.pdf", totalPages = 10, status = MangaDownloadEntity.STATUS_DOWNLOADING, lastHeartbeatAt = 0L))

        // 2. Stuck item with heartbeat 10 mins ago
        val stuckId = dao.insert(MangaDownloadEntity(seriesTitle = "S", chapterTitle = "Ch 2", chapterUrl = "https://crotpedia.net/baca/stuck", outputFileName = "2.pdf", totalPages = 10, status = MangaDownloadEntity.STATUS_DOWNLOADING, lastHeartbeatAt = stuckTime))

        // 3. Active item with recent heartbeat
        val activeId = dao.insert(MangaDownloadEntity(seriesTitle = "S", chapterTitle = "Ch 3", chapterUrl = "https://crotpedia.net/baca/active", outputFileName = "3.pdf", totalPages = 10, status = MangaDownloadEntity.STATUS_DOWNLOADING, lastHeartbeatAt = now))

        // 4. Queued item
        val queuedId = dao.insert(MangaDownloadEntity(seriesTitle = "S", chapterTitle = "Ch 4", chapterUrl = "https://crotpedia.net/baca/queued", outputFileName = "4.pdf", totalPages = 10, status = MangaDownloadEntity.STATUS_QUEUED, lastHeartbeatAt = 0L))

        // Run recovery with threshold 5 mins ago
        val stuckThreshold = now - 300000L
        val resetCount = dao.resetStuckDownloadingItems(stuckThreshold)

        assertEquals(2, resetCount) // legacy and stuck reset
        assertEquals(MangaDownloadEntity.STATUS_QUEUED, dao.getById(legacyId)!!.status)
        assertEquals(MangaDownloadEntity.STATUS_QUEUED, dao.getById(stuckId)!!.status)
        assertEquals(MangaDownloadEntity.STATUS_DOWNLOADING, dao.getById(activeId)!!.status) // active untouched
        assertEquals(MangaDownloadEntity.STATUS_QUEUED, dao.getById(queuedId)!!.status) // queued untouched
    }

    @Test
    fun testPdfCreationCancellationCallback() = runBlocking {
        var cancelled = false
        var iterations = 0

        try {
            PdfBuilder.createPdfFromImages(
                context = context,
                imageFiles = listOf(File(context.cacheDir, "nonexistent.jpg")),
                outputFileName = "test.pdf",
                expectedPages = 1,
                isCancelledCheck = {
                    iterations++
                    if (iterations >= 1) cancelled = true
                    cancelled
                }
            )
        } catch (e: Exception) {
            assertTrue(e is CancellationException)
        }
        assertTrue(cancelled)
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
