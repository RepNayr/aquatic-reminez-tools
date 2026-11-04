package com.repp.aquaticremineztools.domain.compressor.engine

import android.content.Context
import com.repp.aquaticremineztools.data.compressor.CompressionConfig
import java.io.File

data class CompressionProviderResult(
    val isSuccess: Boolean,
    val outputFile: File?,
    val outputWidth: Int,
    val outputHeight: Int,
    val outputFormat: String,
    val isNotBeneficial: Boolean = false,
    val detectedCategory: String? = null,
    val targetSizeAchieved: Boolean? = null,
    val processingTimeMs: Long = 0L,
    val errorMessage: String? = null
)

interface CompressionProvider {
    val name: String
    val isCloud: Boolean
    suspend fun compress(
        context: Context,
        inputFile: File,
        outputFile: File,
        config: CompressionConfig
    ): CompressionProviderResult
}
