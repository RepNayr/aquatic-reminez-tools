package com.repp.aquaticremineztools.presentation.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.video.VideoDownloadEntity
import java.util.Locale

class VideoDownloadAdapter(
    private val onCancelClick: (VideoDownloadEntity) -> Unit,
    private val onRetryClick: (VideoDownloadEntity) -> Unit
) : RecyclerView.Adapter<VideoDownloadAdapter.ViewHolder>() {

    private val items = mutableListOf<VideoDownloadEntity>()

    fun setItems(list: List<VideoDownloadEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manga_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onCancelClick, onRetryClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvDownloadItemTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvDownloadItemInfo)
        private val tvPercent: TextView = itemView.findViewById(R.id.tvDownloadItemPercent)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarItem)
        private val btnCancel: ImageView = itemView.findViewById(R.id.btnCancelItem)

        fun bind(
            item: VideoDownloadEntity,
            onCancelClick: (VideoDownloadEntity) -> Unit,
            onRetryClick: (VideoDownloadEntity) -> Unit
        ) {
            tvTitle.text = "${item.episodeTitle} · ${item.qualityLabel}"

            val isCompressing = item.stage == VideoDownloadEntity.STAGE_COMPRESSING_VIDEO

            when (item.status) {
                VideoDownloadEntity.STATUS_DOWNLOADING -> {
                    btnCancel.visibility = View.VISIBLE
                    progressBar.visibility = View.VISIBLE

                    val downloadedMb = item.downloadedBytes / (1024f * 1024f)
                    val totalMb = item.totalBytes / (1024f * 1024f)
                    val speedKbps = item.downloadSpeedBps / 1024f

                    if (isCompressing) {
                        tvSubtitle.text = "Compressing target ${item.totalBytes / (1024 * 1024)} MB..."
                        tvPercent.text = "${item.progressPercent}%"
                    } else {
                        tvSubtitle.text = String.format(Locale.US, "%.1f MB / %.1f MB (%.1f KB/s)", downloadedMb, totalMb, speedKbps)
                        tvPercent.text = "${item.progressPercent}%"
                    }

                    progressBar.isIndeterminate = item.totalBytes <= 0
                    if (item.totalBytes > 0) {
                        progressBar.progress = item.progressPercent
                    }
                }
                VideoDownloadEntity.STATUS_QUEUED -> {
                    btnCancel.visibility = View.VISIBLE
                    progressBar.visibility = View.VISIBLE
                    progressBar.isIndeterminate = true
                    tvSubtitle.text = if (isCompressing) "Queued for compression..." else "Queued in FIFO line..."
                    tvPercent.text = "QUEUED"
                }
                VideoDownloadEntity.STATUS_FAILED -> {
                    btnCancel.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    tvSubtitle.text = item.errorMessage ?: "Download failed"
                    tvPercent.text = "FAILED"
                }
                VideoDownloadEntity.STATUS_CANCELLED -> {
                    btnCancel.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    tvSubtitle.text = "Cancelled"
                    tvPercent.text = "CANCELLED"
                }
            }

            btnCancel.setOnClickListener {
                if (item.status == VideoDownloadEntity.STATUS_FAILED || item.status == VideoDownloadEntity.STATUS_CANCELLED) {
                    onRetryClick(item)
                } else {
                    onCancelClick(item)
                }
            }
        }
    }
}
