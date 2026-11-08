package com.repp.aquaticremineztools.presentation.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.manga.MangaResultEntity
import java.util.Locale

class MangaResultAdapter(
    private val onCompressClick: (MangaResultEntity) -> Unit,
    private val onSaveClick: (MangaResultEntity) -> Unit,
    private val onRemoveClick: (MangaResultEntity) -> Unit
) : RecyclerView.Adapter<MangaResultAdapter.ViewHolder>() {

    private val items = mutableListOf<MangaResultEntity>()

    fun setItems(list: List<MangaResultEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manga_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onCompressClick, onSaveClick, onRemoveClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvResultStatusBadge)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvResultTitle)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvResultInfo)
        private val btnCompress: Button = itemView.findViewById(R.id.btnResultCompress)
        private val btnSave: Button = itemView.findViewById(R.id.btnResultSave)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemoveResult)

        fun bind(
            result: MangaResultEntity,
            onCompressClick: (MangaResultEntity) -> Unit,
            onSaveClick: (MangaResultEntity) -> Unit,
            onRemoveClick: (MangaResultEntity) -> Unit
        ) {
            tvTitle.text = if (result.seriesTitle.isNotBlank()) "${result.seriesTitle} - ${result.chapterTitle}" else result.chapterTitle

            val sizeMb = result.sizeBytes / (1024f * 1024f)
            tvInfo.text = String.format(Locale.US, "%d pages · %.1f MB", result.pageCount, sizeMb)

            tvStatusBadge.text = when (result.status) {
                MangaResultEntity.STATUS_NOT_COMPLETED -> "NOT COMPLETED"
                MangaResultEntity.STATUS_COMPRESSING -> "COMPRESSING"
                MangaResultEntity.STATUS_COMPLETED -> "COMPLETED"
                MangaResultEntity.STATUS_FAILED -> "FAILED"
                else -> result.status
            }

            btnCompress.isEnabled = result.status != MangaResultEntity.STATUS_COMPRESSING
            btnCompress.text = if (result.status == MangaResultEntity.STATUS_COMPRESSING) "COMPRESSING..." else "COMPRESS"

            btnCompress.setOnClickListener { onCompressClick(result) }
            btnSave.setOnClickListener { onSaveClick(result) }
            btnRemove.setOnClickListener { onRemoveClick(result) }
        }
    }
}
