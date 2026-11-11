package com.repp.aquaticremineztools.data.video.model

data class VideoSeries(
    val id: String,
    val title: String,
    val coverUrl: String,
    val description: String,
    val episodes: List<VideoEpisode>
)
