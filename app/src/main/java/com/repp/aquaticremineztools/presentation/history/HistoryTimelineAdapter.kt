package com.repp.aquaticremineztools.presentation.history

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.ToolIconRegistry
import com.repp.aquaticremineztools.data.local.history.HistoryEntity
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class HistoryTimelineItem {
    data class Header(val dateText: String) : HistoryTimelineItem()
    data class Card(val entity: HistoryEntity, val isSelected: Boolean) : HistoryTimelineItem()
}

class HistoryTimelineAdapter(
    private val blurTarget: BlurTarget,
    private val frameClearDrawable: Drawable?,
    private val onItemClick: (HistoryEntity) -> Unit,
    private val onItemLongClick: (HistoryEntity) -> Unit,
    private val onCheckBoxChanged: (HistoryEntity, Boolean) -> Unit,
    private val onOverflowClick: (HistoryEntity, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<HistoryTimelineItem>()
    var isSelectionMode = false

    fun submitItems(newItems: List<HistoryTimelineItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CARD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryTimelineItem.Header -> VIEW_TYPE_HEADER
            is HistoryTimelineItem.Card -> VIEW_TYPE_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_history_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_history_card, parent, false)
            CardViewHolder(view, blurTarget, frameClearDrawable)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryTimelineItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is HistoryTimelineItem.Card -> {
                (holder as CardViewHolder).bind(item, isSelectionMode, onItemClick, onItemLongClick, onCheckBoxChanged, onOverflowClick)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tvDateHeader)
        fun bind(item: HistoryTimelineItem.Header) {
            tvDateHeader.text = item.dateText
        }
    }

    class CardViewHolder(
        itemView: View,
        blurTarget: BlurTarget,
        frameClearDrawable: Drawable?
    ) : RecyclerView.ViewHolder(itemView) {
        private val blurCard: BlurView = itemView.findViewById(R.id.blurCard)
        private val cardContent: View = itemView.findViewById(R.id.cardContent)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
        private val ivToolIcon: ImageView = itemView.findViewById(R.id.ivToolIcon)
        private val tvToolName: TextView = itemView.findViewById(R.id.tvToolName)
        private val statusChip: LinearLayout = itemView.findViewById(R.id.statusChip)
        private val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        private val tvStatusLabel: TextView = itemView.findViewById(R.id.tvStatusLabel)
        private val btnOverflow: FrameLayout = itemView.findViewById(R.id.btnOverflow)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
        private val tvTimeDuration: TextView = itemView.findViewById(R.id.tvTimeDuration)

        init {
            GlassBlurHelper.setupBlur(blurCard, blurTarget, frameClearDrawable)
        }

        fun bind(
            item: HistoryTimelineItem.Card,
            selectionMode: Boolean,
            onItemClick: (HistoryEntity) -> Unit,
            onItemLongClick: (HistoryEntity) -> Unit,
            onCheckBoxChanged: (HistoryEntity, Boolean) -> Unit,
            onOverflowClick: (HistoryEntity, View) -> Unit
        ) {
            val entity = item.entity
            val context = itemView.context

            tvToolName.text = entity.toolName
            tvCategory.text = "${entity.category} · ${if (entity.activityType == "EXECUTED") "Executed" else "Opened"}"
            tvSummary.text = entity.resultSummary ?: "No details recorded."

            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entity.timestamp))
            val durationStr = if (entity.durationMs != null && entity.durationMs > 0) {
                val sec = entity.durationMs / 1000.0
                String.format(Locale.getDefault(), " · %.1f sec", sec)
            } else ""
            tvTimeDuration.text = "$timeStr$durationStr"

            // Tool Icon mapped by toolId
            val iconRes = ToolIconRegistry.getIconRes(entity.toolId)
            ivToolIcon.setImageResource(iconRes)
            ivToolIcon.imageTintList = null
            ivToolIcon.clearColorFilter()

            // Status chip
            if (entity.activityType == "OPENED") {
                statusChip.visibility = View.VISIBLE
                ivStatusIcon.setImageResource(R.drawable.ic_info)
                ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
                tvStatusLabel.text = "Opened"
                tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            } else {
                statusChip.visibility = View.VISIBLE
                when (entity.status) {
                    "SUCCESS" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_status_success)
                        ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_online))
                        tvStatusLabel.text = "Success"
                        tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.status_online))
                    }
                    "FAILED" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_status_failed)
                        ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_error))
                        tvStatusLabel.text = "Failed"
                        tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.status_error))
                    }
                    "RUNNING" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_status_running)
                        ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_blue))
                        tvStatusLabel.text = "Running"
                        tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
                    }
                    "CANCELLED" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_status_cancel)
                        ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_warning))
                        tvStatusLabel.text = "Cancelled"
                        tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                    }
                    "INTERRUPTED" -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_status_interrupted)
                        ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_violet))
                        tvStatusLabel.text = "Interrupted"
                        tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.accent_violet))
                    }
                    else -> {
                        statusChip.visibility = View.GONE
                    }
                }
            }

            // Selection mode
            if (selectionMode) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = item.isSelected
                btnOverflow.visibility = View.GONE
            } else {
                cbSelect.visibility = View.GONE
                btnOverflow.visibility = View.VISIBLE
            }

            cbSelect.setOnClickListener {
                onCheckBoxChanged(entity, cbSelect.isChecked)
            }

            cardContent.setOnClickListener {
                if (selectionMode) {
                    cbSelect.isChecked = !cbSelect.isChecked
                    onCheckBoxChanged(entity, cbSelect.isChecked)
                } else {
                    onItemClick(entity)
                }
            }

            cardContent.setOnLongClickListener {
                onItemLongClick(entity)
                true
            }

            btnOverflow.setOnClickListener { view ->
                onOverflowClick(entity, view)
            }
        }
    }
}
