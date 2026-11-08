package com.repp.aquaticremineztools.presentation.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import java.util.Locale

class MangaDownloadAdapter(
    private val onCancelClick: (MangaDownloadEntity) -> Unit,
    private val onRetryClick: (MangaDownloadEntity) -> Unit
) : RecyclerView.Adapter<MangaDownloadAdapter.ViewHolder>() {

    private val items = mutableListOf<MangaDownloadEntity>()

    fun setItems(list: List<MangaDownloadEntity>) {
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
        private val btnCancel: ImageView = itemView.findViewById(R.id.btnCancelItem)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarItem)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvDownloadItemInfo)
        private val tvPercent: TextView = itemView.findViewById(R.id.tvDownloadItemPercent)

        fun bind(entity: MangaDownloadEntity, onCancelClick: (MangaDownloadEntity) -> Unit, onRetryClick: (MangaDownloadEntity) -> Unit) {
            tvTitle.text = entity.chapterTitle

            btnCancel.setOnClickListener {
                onCancelClick(entity)
            }

            itemView.setOnClickListener {
                if (entity.status == MangaDownloadEntity.STATUS_FAILED) {
                    onRetryClick(entity)
                }
            }

            if (entity.status == MangaDownloadEntity.STATUS_QUEUED) {
                progressBar.isIndeterminate = true
                tvPercent.text = "QUEUED"
                tvInfo.text = "Waiting in queue..."
            } else if (entity.status == MangaDownloadEntity.STATUS_FAILED) {
                progressBar.isIndeterminate = false
                progressBar.progress = 0
                tvPercent.text = "FAILED"
                tvInfo.text = entity.errorMessage ?: "Download failed. Tap to retry."
            } else {
                progressBar.isIndeterminate = false
                val percent = if (entity.totalPages > 0) {
                    (entity.downloadedPages * 100) / entity.totalPages
                } else 0

                progressBar.progress = percent.coerceIn(0, 100)
                tvPercent.text = "$percent%"

                val downloadedMb = entity.downloadedBytes / (1024f * 1024f)
                val totalMb = entity.totalBytes / (1024f * 1024f)
                val speedMb = entity.downloadSpeedBps / (1024f * 1024f)

                tvInfo.text = String.format(
                    Locale.US,
                    "%.1f MB / %.1f MB (%.1f MB/s)",
                    downloadedMb,
                    totalMb,
                    speedMb
                )
            }
        }
    }
}
