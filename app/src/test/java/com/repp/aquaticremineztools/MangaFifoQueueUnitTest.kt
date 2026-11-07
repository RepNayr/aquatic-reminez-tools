package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaFifoQueueUnitTest {

    @Test
    fun testFifoOrdering() {
        val queue = mutableListOf<MangaDownloadEntity>()

        val item1 = MangaDownloadEntity(id = 1, seriesTitle = "Manga", chapterTitle = "Chapter 1", chapterUrl = "http://crotpedia.net/ch1", outputFileName = "ch1.pdf", totalPages = 20)
        val item2 = MangaDownloadEntity(id = 2, seriesTitle = "Manga", chapterTitle = "Chapter 2", chapterUrl = "http://crotpedia.net/ch2", outputFileName = "ch2.pdf", totalPages = 24)
        val item3 = MangaDownloadEntity(id = 3, seriesTitle = "Manga", chapterTitle = "Chapter 5", chapterUrl = "http://crotpedia.net/ch5", outputFileName = "ch5.pdf", totalPages = 30)

        queue.add(item1)
        queue.add(item2)
        queue.add(item3)

        // FIFO: First item added must be first processed
        val firstProcessed = queue.removeAt(0)
        assertEquals("Chapter 1", firstProcessed.chapterTitle)

        val secondProcessed = queue.removeAt(0)
        assertEquals("Chapter 2", secondProcessed.chapterTitle)

        val thirdProcessed = queue.removeAt(0)
        assertEquals("Chapter 5", thirdProcessed.chapterTitle)
    }

    @Test
    fun testDeduplicationLogic() {
        val existingUrls = setOf("http://crotpedia.net/ch1", "http://crotpedia.net/ch2")

        val newUrl1 = "http://crotpedia.net/ch1"
        val newUrl3 = "http://crotpedia.net/ch3"

        assertTrue(existingUrls.contains(newUrl1))
        assertFalse(existingUrls.contains(newUrl3))
    }
}
