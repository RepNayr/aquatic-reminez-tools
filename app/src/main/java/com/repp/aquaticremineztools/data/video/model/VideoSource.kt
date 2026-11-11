package com.repp.aquaticremineztools.data.video.model

data class VideoSource(
    val serverId: String,
    val serverName: String,
    val quality: VideoQuality,
    val downloadPageUrl: String,
    val mediaFormat: String = "MP4",
    val refererHeader: String = ""
)
