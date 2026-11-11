package com.repp.aquaticremineztools.presentation.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.video.VideoResultEntity
import java.util.Locale

class VideoResultAdapter(
    private val onPlayClick: (VideoResultEntity) -> Unit,
    private val onCompressClick: (VideoResultEntity, targetSizeBytes: Long) -> Unit,
    private val onSaveClick: (VideoResultEntity) -> Unit,
    private val onRemoveClick: (VideoResultEntity) -> Unit
) : RecyclerView.Adapter<VideoResultAdapter.ViewHolder>() {

    private val items = mutableListOf<VideoResultEntity>()

    fun setItems(list: List<VideoResultEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onPlayClick, onCompressClick, onSaveClick, onRemoveClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvResultStatusBadge)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvResultTitle)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvResultInfo)
        private val tvTargetSizeLabel: TextView = itemView.findViewById(R.id.tvTargetSizeLabel)
        private val seekBarTargetSize: SeekBar = itemView.findViewById(R.id.seekBarTargetSize)
        private val btnPlay: Button = itemView.findViewById(R.id.btnResultPlay)
        private val btnCompress: Button = itemView.findViewById(R.id.btnResultCompress)
        private val btnSave: Button = itemView.findViewById(R.id.btnResultSave)
        private val btnRemove: Button = itemView.findViewById(R.id.btnResultRemove)
        private val btnRemoveTop: ImageView = itemView.findViewById(R.id.btnRemoveResultTop)

        private var selectedTargetMb: Int = 100

        fun bind(
            result: VideoResultEntity,
            onPlayClick: (VideoResultEntity) -> Unit,
            onCompressClick: (VideoResultEntity, Long) -> Unit,
            onSaveClick: (VideoResultEntity) -> Unit,
            onRemoveClick: (VideoResultEntity) -> Unit
        ) {
            tvTitle.text = if (result.seriesTitle.isNotBlank()) {
                "${result.episodeTitle} · ${result.seriesTitle}"
            } else {
                result.episodeTitle
            }

            val currentMb = result.sizeBytes / (1024f * 1024f)
            val origMb = result.originalSizeBytes / (1024f * 1024f)

            tvInfo.text = if (origMb > 0f && Math.abs(origMb - currentMb) > 0.1f && result.status == VideoResultEntity.STATUS_COMPLETED) {
                String.format(Locale.US, "%s MP4 · Before: %.1f MB → After: %.1f MB", result.resolutionLabel, origMb, currentMb)
            } else {
                String.format(Locale.US, "%s MP4 · %.1f MB", result.resolutionLabel, currentMb)
            }

            tvStatusBadge.text = when (result.status) {
                VideoResultEntity.STATUS_COMPRESSING -> "COMPRESSING"
                VideoResultEntity.STATUS_COMPLETED -> "COMPLETED"
                VideoResultEntity.STATUS_FAILED -> "FAILED"
                else -> result.status
            }

            val isCompressing = result.status == VideoResultEntity.STATUS_COMPRESSING

            // Configure SeekBar (10 MB to 500 MB: progress 0 = 10 MB, progress 490 = 500 MB)
            seekBarTargetSize.progress = (selectedTargetMb - 10).coerceIn(0, 490)
            tvTargetSizeLabel.text = String.format(Locale.US, "%d MB", selectedTargetMb)

            seekBarTargetSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedTargetMb = progress + 10
                    tvTargetSizeLabel.text = String.format(Locale.US, "%d MB", selectedTargetMb)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            val alpha = if (isCompressing) 0.5f else 1.0f
            itemView.alpha = alpha

            btnPlay.isEnabled = !isCompressing
            btnCompress.isEnabled = !isCompressing
            btnSave.isEnabled = !isCompressing
            btnRemove.isEnabled = !isCompressing
            btnRemoveTop.isEnabled = !isCompressing
            seekBarTargetSize.isEnabled = !isCompressing

            btnCompress.text = if (isCompressing) "COMPRESSING..." else "COMPRESS"

            btnPlay.setOnClickListener { onPlayClick(result) }
            btnCompress.setOnClickListener {
                onCompressClick(result, selectedTargetMb.toLong() * 1024L * 1024L)
            }
            btnSave.setOnClickListener { onSaveClick(result) }
            btnRemove.setOnClickListener { onRemoveClick(result) }
            btnRemoveTop.setOnClickListener { onRemoveClick(result) }
        }
    }
}
