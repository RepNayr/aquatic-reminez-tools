package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.ToolData
import com.repp.aquaticremineztools.data.ToolImplementationHelper
import com.repp.aquaticremineztools.data.compressor.CompressionConfig
import com.repp.aquaticremineztools.data.compressor.CompressionMode
import com.repp.aquaticremineztools.data.compressor.CompressionPreset
import com.repp.aquaticremineztools.data.compressor.OutputFormat
import com.repp.aquaticremineztools.data.compressor.ResolutionPreset
import com.repp.aquaticremineztools.data.compressor.TargetSizePriority
import com.repp.aquaticremineztools.domain.compressor.ImageCategory
import com.repp.aquaticremineztools.domain.compressor.ImageContentDetector
import com.repp.aquaticremineztools.domain.compressor.ImageEnhancer
import com.repp.aquaticremineztools.domain.compressor.ImageHashManager
import com.repp.aquaticremineztools.domain.compressor.ImageImportManager
import com.repp.aquaticremineztools.domain.compressor.ImageQualityValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertEquals(100 * 1024 * 1024L, ImageImportManager.MAX_FILE_SIZE_BYTES)
        assertEquals(200 * 1024 * 1024L, ImageImportManager.MAX_BATCH_SIZE_BYTES)
    }

    @Test
    fun testSha256AndDuplicateDetection() {
        runBlocking {
            val tempFile = File.createTempFile("test_hash_", ".txt")
            tempFile.writeText("Aquatic Reminez Image Compressor SHA-256 Test Content")

            val hash1 = ImageHashManager.computeSha256(tempFile)
            assertNotNull(hash1)

            val dupFirst = ImageHashManager.registerHashAndCheckDuplicate("item1", hash1!!)
            assertNull("First registration should not be duplicate", dupFirst)

            val dupSecond = ImageHashManager.registerHashAndCheckDuplicate("item2", hash1)
            assertEquals("item1", dupSecond)

            ImageHashManager.unregisterHash("item1")
            val dupAfterRemove = ImageHashManager.registerHashAndCheckDuplicate("item3", hash1)
            assertNull("After removal, re-registering should be permitted", dupAfterRemove)

            ImageHashManager.clearSessionHashes()
            tempFile.delete()
        }
    }

    @Test
    fun testCompressionPresetsAndModes() {
        val configDefault = CompressionConfig()
        assertEquals(CompressionMode.AUTO, configDefault.mode)
        assertEquals(75, configDefault.quality)
        assertEquals(30, configDefault.minQuality)
        assertEquals(OutputFormat.ORIGINAL, configDefault.outputFormat)

        assertEquals(88, CompressionPreset.LIGHT.initialQuality)
        assertEquals(75, CompressionPreset.BALANCED.initialQuality)
        assertEquals(55, CompressionPreset.STRONG.initialQuality)
        assertEquals(35, CompressionPreset.EXTREME.initialQuality)
        assertEquals(78, CompressionPreset.SMART_AUTO.initialQuality)

        assertNotNull(OutputFormat.valueOf("SMART"))
        assertNotNull(OutputFormat.valueOf("AUTO_BEST"))
        assertNotNull(OutputFormat.valueOf("AVIF"))
    }

    @Test
    fun testTargetSizePriorityAndResolutionPresets() {
        assertEquals("Quality First", TargetSizePriority.QUALITY_FIRST.displayName)
        assertEquals("Size First", TargetSizePriority.SIZE_FIRST.displayName)
        assertEquals("Balanced", TargetSizePriority.BALANCED.displayName)

        assertEquals(0, ResolutionPreset.ORIGINAL.targetLongSide)
        assertEquals(3840, ResolutionPreset.RES_4K.targetLongSide)
        assertEquals(1920, ResolutionPreset.RES_1080P.targetLongSide)
        assertEquals(1280, ResolutionPreset.RES_720P.targetLongSide)
        assertEquals(854, ResolutionPreset.RES_480P.targetLongSide)
    }

    @Test
    fun testQualityValidatorNoFakeMetricsForInvalidFiles() {
        val dummyOriginal = File.createTempFile("dummy_orig_", ".jpg")
        val dummyCompressed = File.createTempFile("dummy_comp_", ".jpg")
        dummyOriginal.writeText("Corrupted or invalid image data")
        dummyCompressed.writeText("Corrupted or invalid image data")

        val metrics = ImageQualityValidator.evaluateQuality(dummyOriginal, dummyCompressed)

        assertNull("PSNR must be null when image decoding fails, no fake defaults allowed", metrics.psnrDb)
        assertNull("SSIM must be null when image decoding fails, no fake defaults allowed", metrics.ssim)
        assertFalse("Unreadable images must not be marked acceptable", metrics.isAcceptable)

        dummyOriginal.delete()
        dummyCompressed.delete()
    }

    @Test
    fun testImageContentDetectorNonExistentFile() {
        val dummyFile = File("non_existent_image_file.png")
        val category = ImageContentDetector.detectCategory(dummyFile)
        assertEquals("Non-existent file defaults safely to Photo category", ImageCategory.PHOTO, category)
    }

    @Test
    fun testToolCatalogData() {
        val compressorTool = ToolData.ALL_TOOLS.find { it.id == "media_image_compressor" }
        assertNotNull("Image Compressor must exist in ToolData.ALL_TOOLS", compressorTool)
        assertEquals("Image Compressor", compressorTool?.name)
        assertEquals(ToolData.Category.MEDIA, compressorTool?.category)
    }
}
