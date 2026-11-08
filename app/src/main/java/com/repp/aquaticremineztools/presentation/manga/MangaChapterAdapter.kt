package com.repp.aquaticremineztools.presentation.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.manga.model.ChapterAnalysisStatus
import com.repp.aquaticremineztools.data.manga.model.MangaChapter

class MangaChapterAdapter(
    private val onDownloadClick: (MangaChapter) -> Unit,
    private val onRetryClick: ((MangaChapter) -> Unit)? = null
) : RecyclerView.Adapter<MangaChapterAdapter.ViewHolder>() {

    private val chapters = mutableListOf<MangaChapter>()

    fun setChapters(list: List<MangaChapter>) {
        chapters.clear()
        chapters.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manga_chapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = chapters[position]
        holder.bind(item, onDownloadClick, onRetryClick)
    }

    override fun getItemCount(): Int = chapters.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvChapterTitle)
        private val btnRetry: ImageView = itemView.findViewById(R.id.btnRetryChapter)
        private val pbRetryLoading: ProgressBar = itemView.findViewById(R.id.pbRetryLoading)
        private val tvPages: TextView = itemView.findViewById(R.id.tvChapterPages)
        private val btnDownload: Button = itemView.findViewById(R.id.btnDownloadChapter)

        fun bind(
            chapter: MangaChapter,
            onDownloadClick: (MangaChapter) -> Unit,
            onRetryClick: ((MangaChapter) -> Unit)?
        ) {
            tvTitle.text = chapter.title

            when (chapter.status) {
                ChapterAnalysisStatus.ANALYZING -> {
                    tvPages.text = "Menganalisis..."
                    tvPages.setTextColor(0xFF746D62.toInt())
                    btnRetry.visibility = View.GONE
                    pbRetryLoading.visibility = View.VISIBLE
                    btnDownload.isEnabled = false
                    btnDownload.alpha = 0.4f
                }
                ChapterAnalysisStatus.SUCCESS -> {
                    tvPages.text = if (chapter.pageCount >= 0) "${chapter.pageCount} halaman" else "0 halaman"
                    tvPages.setTextColor(0xFF746D62.toInt())
                    btnRetry.visibility = View.GONE
                    pbRetryLoading.visibility = View.GONE
                    btnDownload.isEnabled = true
                    btnDownload.alpha = 1.0f
                }
                ChapterAnalysisStatus.FAILED -> {
                    tvPages.text = "Failed"
                    tvPages.setTextColor(0xFFD32F2F.toInt())
                    btnRetry.visibility = View.VISIBLE
                    pbRetryLoading.visibility = View.GONE
                    btnDownload.isEnabled = false
                    btnDownload.alpha = 0.4f
                }
                ChapterAnalysisStatus.PENDING -> {
                    tvPages.text = "Belum dianalisis"
                    tvPages.setTextColor(0xFF746D62.toInt())
                    btnRetry.visibility = View.GONE
                    pbRetryLoading.visibility = View.GONE
                    btnDownload.isEnabled = false
                    btnDownload.alpha = 0.4f
                }
            }

            btnRetry.setOnClickListener {
                onRetryClick?.invoke(chapter)
            }

            btnDownload.setOnClickListener {
                if (chapter.status == ChapterAnalysisStatus.SUCCESS) {
                    onDownloadClick(chapter)
                }
            }
        }
    }
}
