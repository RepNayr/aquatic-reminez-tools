package com.repp.aquaticremineztools.presentation.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.video.model.VideoEpisode

class VideoEpisodeAdapter(
    private val onEpisodeClick: (VideoEpisode) -> Unit,
    private val onDownloadClick: (VideoEpisode) -> Unit
) : RecyclerView.Adapter<VideoEpisodeAdapter.EpisodeViewHolder>() {

    private val episodes = mutableListOf<VideoEpisode>()

    fun setEpisodes(newEpisodes: List<VideoEpisode>) {
        episodes.clear()
        episodes.addAll(newEpisodes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_episode, parent, false)
        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(episodes[position])
    }

    override fun getItemCount(): Int = episodes.size

    inner class EpisodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivEpisodeThumbnail)
        private val tvPlaceholder: TextView = itemView.findViewById(R.id.tvThumbnailPlaceholder)
        private val tvEpisodeNumber: TextView = itemView.findViewById(R.id.tvEpisodeNumber)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvEpisodeTitle)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvEpisodeDuration)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvEpisodeConfigStatus)
        private val btnDownload: Button = itemView.findViewById(R.id.btnDownloadEpisode)

        fun bind(episode: VideoEpisode) {
            tvEpisodeNumber.text = "EPISODE ${episode.order}"
            tvEpisodeNumber.setTextColor(0xFF292722.toInt())
            tvTitle.text = episode.title

            if (episode.durationFormatted.isNotBlank()) {
                tvDuration.visibility = View.VISIBLE
                tvDuration.text = "Duration: ${episode.durationFormatted}"
            } else {
                tvDuration.visibility = View.GONE
            }

            if (episode.thumbnailUrl.isNotBlank()) {
                ivThumbnail.load(episode.thumbnailUrl) {
                    crossfade(true)
                    addHeader("User-Agent", com.repp.aquaticremineztools.data.video.source.HentaiCopParser.USER_AGENT)
                    addHeader("Referer", "https://hentaicop.com/")
                }
                tvPlaceholder.visibility = View.GONE
            } else {
                ivThumbnail.setImageDrawable(null)
                tvPlaceholder.visibility = View.VISIBLE
            }

            val savedQuality = episode.savedQualityLabel ?: "720p"
            val hasAvailable720p = episode.availableSources.any { it.quality.height == 720 }
            val hasValidConfig = episode.savedQualityLabel != null || hasAvailable720p

            if (hasValidConfig) {
                tvStatus.text = "$savedQuality • Ready"
                tvStatus.setTextColor(0xFF2E7D32.toInt())
            } else {
                tvStatus.text = "Quality Setup Required"
                tvStatus.setTextColor(0xFFD32F2F.toInt())
            }

            itemView.setOnClickListener {
                onEpisodeClick(episode)
            }

            btnDownload.setOnClickListener {
                onDownloadClick(episode)
            }
        }
    }
}
