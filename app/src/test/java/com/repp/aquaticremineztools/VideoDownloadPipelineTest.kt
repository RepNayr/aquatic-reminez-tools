package com.repp.aquaticremineztools

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repp.aquaticremineztools.data.local.video.VideoDatabase
import com.repp.aquaticremineztools.data.local.video.VideoDownloadEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VideoDownloadPipelineTest {

    private lateinit var db: VideoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VideoDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFifoQueueOrdering() = runBlocking {
        val dao = db.videoDownloadDao()

        val item1 = VideoDownloadEntity(
            seriesTitle = "Series 1",
            episodeTitle = "Episode 1",
            episodeUrl = "https://hentaicop.com/ep-1/",
            downloadPageUrl = "https://hentaicop.com/unduh/ep-1/",
            serverId = "server_1",
            qualityLabel = "720p",
            outputFileName = "ep_1.mp4"
        )

        val item2 = VideoDownloadEntity(
            seriesTitle = "Series 1",
            episodeTitle = "Episode 2",
            episodeUrl = "https://hentaicop.com/ep-2/",
            downloadPageUrl = "https://hentaicop.com/unduh/ep-2/",
            serverId = "server_1",
            qualityLabel = "720p",
            outputFileName = "ep_2.mp4"
        )

        dao.insert(item1)
        dao.insert(item2)

        val next1 = dao.getNextQueuedItem()
        assertNotNull(next1)
        assertEquals("Episode 1", next1?.episodeTitle)

        // Claim item 1
        dao.claimQueuedItem(next1!!.id)

        val next2 = dao.getNextQueuedItem()
        assertNotNull(next2)
        assertEquals("Episode 2", next2?.episodeTitle)
    }

    @Test
    fun testProgressUpdate() = runBlocking {
        val dao = db.videoDownloadDao()

        val item = VideoDownloadEntity(
            seriesTitle = "Series 1",
            episodeTitle = "Episode 1",
            episodeUrl = "https://hentaicop.com/ep-1/",
            downloadPageUrl = "https://hentaicop.com/unduh/ep-1/",
            serverId = "server_1",
            qualityLabel = "720p",
            outputFileName = "ep_1.mp4"
        )

        val id = dao.insert(item)
        dao.claimQueuedItem(id)

        val updated = dao.updateProgressIfDownloading(
            id = id,
            bytes = 5000000L,
            totalBytes = 10000000L,
            speed = 1000000L,
            percent = 50
        )

        assertEquals(1, updated)

        val fetched = dao.getById(id)
        assertNotNull(fetched)
        assertEquals(5000000L, fetched?.downloadedBytes)
        assertEquals(50, fetched?.progressPercent)
    }
}
