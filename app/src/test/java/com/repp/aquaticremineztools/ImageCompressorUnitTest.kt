package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.ToolData
import com.repp.aquaticremineztools.data.ToolImplementationHelper
import com.repp.aquaticremineztools.data.compressor.CompressionConfig
import com.repp.aquaticremineztools.data.compressor.CompressionMode
import com.repp.aquaticremineztools.data.compressor.CompressionPreset
import com.repp.aquaticremineztools.data.compressor.OutputFormat
import com.repp.aquaticremineztools.domain.compressor.ImageHashManager
import com.repp.aquaticremineztools.domain.compressor.ImageImportManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImageCompressorUnitTest {

    @Test
    fun testToolImplementationStatus() {
        assertTrue(
            "Image Compressor must be marked as implemented",
            ToolImplementationHelper.isToolImplemented("media_image_compressor")
        )
        assertFalse(
            "Unrelated tools must remain not implemented",
            ToolImplementationHelper.isToolImplemented("util_calculator")
        )
        assertFalse(
            "Unrelated media tools must remain not implemented",
            ToolImplementationHelper.isToolImplemented("media_color_picker")
        )
    }

    @Test
    fun testImportLimits() {
        assertEquals(10, ImageImportManager.MAX_FILES_PER_BATCH)
        assertEquals(50 * 1024 * 1024L, ImageImportManager.MAX_FILE_SIZE_BYTES)
        assertEquals(200 * 1024 * 1024L, ImageImportManager.MAX_BATCH_SIZE_BYTES)
    }

    @Test
    fun testSha256AndDuplicateDetection() {
        runBlocking {
            val tempFile = File.createTempFile("test_hash_", ".txt")
            tempFile.writeText("Aquatic Reminez Image Compressor SHA-256 Test Content")

            val hash1 = ImageHashManager.computeSha256(tempFile)
            assertNotNull(hash1)

            val isDupFirst = ImageHashManager.registerHashAndCheckDuplicate("item1", hash1!!)
            assertFalse("First registration should not be duplicate", isDupFirst)

            val isDupSecond = ImageHashManager.registerHashAndCheckDuplicate("item2", hash1)
            assertTrue("Second registration with same hash must be flagged as duplicate", isDupSecond)

            ImageHashManager.unregisterHash("item1")
            val isDupAfterRemove = ImageHashManager.registerHashAndCheckDuplicate("item3", hash1)
            assertFalse("After removal, re-registering should be permitted", isDupAfterRemove)

            ImageHashManager.clearSessionHashes()
            tempFile.delete()
        }
    }

    @Test
    fun testCompressionPresetsAndModes() {
        val configDefault = CompressionConfig()
        assertEquals(CompressionMode.AUTO, configDefault.mode)
        assertEquals(75, configDefault.quality)
        assertEquals(OutputFormat.ORIGINAL, configDefault.outputFormat)

        assertEquals(90, CompressionPreset.LOW.initialQuality)
        assertEquals(75, CompressionPreset.MEDIUM.initialQuality)
        assertEquals(55, CompressionPreset.HIGH.initialQuality)
        assertEquals(35, CompressionPreset.MAXIMUM.initialQuality)
    }

    @Test
    fun testToolCatalogData() {
        val compressorTool = ToolData.ALL_TOOLS.find { it.id == "media_image_compressor" }
        assertNotNull("Image Compressor must exist in ToolData.ALL_TOOLS", compressorTool)
        assertEquals("Image Compressor", compressorTool?.name)
        assertEquals(ToolData.Category.MEDIA, compressorTool?.category)
    }
}
