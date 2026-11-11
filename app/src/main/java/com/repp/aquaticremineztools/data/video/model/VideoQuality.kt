package com.repp.aquaticremineztools.data.video.model

data class VideoQuality(
    val label: String,
    val height: Int = 720,
    val fileSizeBytes: Long = 0L,
    val formattedSize: String = ""
)
