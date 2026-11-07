package com.repp.aquaticremineztools.presentation.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.manga.model.MangaChapter

class MangaChapterAdapter(
    private val onDownloadClick: (MangaChapter) -> Unit
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
        holder.bind(item, onDownloadClick)
    }

    override fun getItemCount(): Int = chapters.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvChapterTitle)
        private val tvPages: TextView = itemView.findViewById(R.id.tvChapterPages)
        private val btnDownload: Button = itemView.findViewById(R.id.btnDownloadChapter)

        fun bind(chapter: MangaChapter, onDownloadClick: (MangaChapter) -> Unit) {
            tvTitle.text = chapter.title
            tvPages.text = "${chapter.pageCount} halaman"

            btnDownload.setOnClickListener {
                onDownloadClick(chapter)
            }
        }
    }
}
