package com.repp.aquaticremineztools.data.local.compressor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compressor_items")
data class CompressorItemEntity(
    @PrimaryKey
    val id: String,
    val uri: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sha256: String?,
    val status: String,
    val queuePosition: Int,
    val progress: Int,
    val errorMessage: String?,
    val originalSize: Long,
    val compressedSize: Long,
    val compressedUri: String?,
    val compressedWidth: Int,
    val compressedHeight: Int,
    val compressedFormat: String?,
    val psnrDb: Double?,
    val ssimScore: Double?,
    val isNotBeneficial: Boolean,
    val savedToGalleryPath: String?,
    val localTempPath: String?,
    val addedTimestamp: Long
)
