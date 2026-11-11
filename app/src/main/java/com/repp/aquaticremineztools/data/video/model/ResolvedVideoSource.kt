package com.repp.aquaticremineztools.data.video.model

data class ResolvedVideoSource(
    val mediaUrl: String,
    val referer: String,
    val userAgent: String,
    val expectedSizeBytes: Long,
    val selectedQuality: VideoQuality,
    val mimeType: String = "video/mp4"
)
