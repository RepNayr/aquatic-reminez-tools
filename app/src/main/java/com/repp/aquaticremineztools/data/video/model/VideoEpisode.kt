package com.repp.aquaticremineztools.data.video.model

data class VideoEpisode(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String = "",
    val durationFormatted: String = "",
    val order: Int = 1,
    val availableSources: List<VideoSource> = emptyList(),
    val savedServerId: String? = null,
    val savedQualityLabel: String? = null
)
