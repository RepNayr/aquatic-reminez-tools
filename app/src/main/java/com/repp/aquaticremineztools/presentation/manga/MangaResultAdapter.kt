package com.repp.aquaticremineztools.presentation.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.manga.MangaResultEntity
import java.util.Locale

class MangaResultAdapter(
    private val onCompressClick: (MangaResultEntity, targetSizeBytes: Long) -> Unit,
    private val onSaveClick: (MangaResultEntity) -> Unit,
    private val onOpenClick: (MangaResultEntity) -> Unit,
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
        holder.bind(item, onCompressClick, onSaveClick, onOpenClick, onRemoveClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvResultStatusBadge)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvResultTitle)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvResultInfo)
        private val tvTargetSizeLabel: TextView = itemView.findViewById(R.id.tvTargetSizeLabel)
        private val seekBarTargetSize: SeekBar = itemView.findViewById(R.id.seekBarTargetSize)
        private val btnCompress: Button = itemView.findViewById(R.id.btnResultCompress)
        private val btnSave: Button = itemView.findViewById(R.id.btnResultSave)
        private val btnOpen: Button = itemView.findViewById(R.id.btnResultOpen)
        private val btnRemove: Button = itemView.findViewById(R.id.btnResultRemove)
        private val btnRemoveTop: ImageView = itemView.findViewById(R.id.btnRemoveResultTop)

        private var selectedTargetMb: Int = 20

        fun bind(
            result: MangaResultEntity,
            onCompressClick: (MangaResultEntity, Long) -> Unit,
            onSaveClick: (MangaResultEntity) -> Unit,
            onOpenClick: (MangaResultEntity) -> Unit,
            onRemoveClick: (MangaResultEntity) -> Unit
        ) {
            tvTitle.text = if (result.seriesTitle.isNotBlank()) {
                "${result.chapterTitle} · ${result.seriesTitle}"
            } else {
                result.chapterTitle
            }

            val currentMb = result.sizeBytes / (1024f * 1024f)
            val origMb = result.originalSizeBytes / (1024f * 1024f)

            tvInfo.text = if (origMb > 0f && Math.abs(origMb - currentMb) > 0.05f && result.status == MangaResultEntity.STATUS_COMPLETED) {
                String.format(Locale.US, "%d pages · Before: %.1f MB → After: %.1f MB", result.pageCount, origMb, currentMb)
            } else {
                String.format(Locale.US, "%d pages · %.1f MB", result.pageCount, currentMb)
            }

            tvStatusBadge.text = when (result.status) {
                MangaResultEntity.STATUS_NOT_COMPLETED -> "NOT COMPLETED"
                MangaResultEntity.STATUS_COMPRESSING -> "COMPRESSING"
                MangaResultEntity.STATUS_COMPLETED -> "COMPLETED"
                MangaResultEntity.STATUS_FAILED -> "FAILED"
                else -> result.status
            }

            val isCompressing = result.status == MangaResultEntity.STATUS_COMPRESSING

            // Configure SeekBar (10 MB to 100 MB: progress 0 = 10, progress 90 = 100)
            seekBarTargetSize.progress = (selectedTargetMb - 10).coerceIn(0, 90)
            tvTargetSizeLabel.text = String.format(Locale.US, "%d MB", selectedTargetMb)

            seekBarTargetSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedTargetMb = progress + 10
                    tvTargetSizeLabel.text = String.format(Locale.US, "%d MB", selectedTargetMb)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Set enabled & alpha state for COMPRESSING
            val alpha = if (isCompressing) 0.5f else 1.0f
            itemView.alpha = alpha

            btnCompress.isEnabled = !isCompressing
            btnSave.isEnabled = !isCompressing
            btnOpen.isEnabled = !isCompressing
            btnRemove.isEnabled = !isCompressing
            btnRemoveTop.isEnabled = !isCompressing
            seekBarTargetSize.isEnabled = !isCompressing

            btnCompress.text = if (isCompressing) "COMPRESSING..." else "COMPRESS"

            btnCompress.setOnClickListener {
                onCompressClick(result, selectedTargetMb.toLong() * 1024L * 1024L)
            }
            btnSave.setOnClickListener { onSaveClick(result) }
            btnOpen.setOnClickListener { onOpenClick(result) }
            btnRemove.setOnClickListener { onRemoveClick(result) }
            btnRemoveTop.setOnClickListener { onRemoveClick(result) }
        }
    }
}
