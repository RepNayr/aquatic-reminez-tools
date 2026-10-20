package com.repp.aquaticremineztools.presentation.history

import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val onCardClick: (HistoryTimelineItem.Card, Int) -> Unit,
    private val onOverflowClick: (HistoryEntity, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<HistoryTimelineItem>()
    var isSelectionMode = false
    private var searchQuery = ""

    fun submitItems(newItems: List<HistoryTimelineItem>, query: String = "") {
        items.clear()
        items.addAll(newItems)
        searchQuery = query
        notifyDataSetChanged()
    }

    fun toggleItemSelection(position: Int) {
        val item = items.getOrNull(position)
        if (item is HistoryTimelineItem.Card) {
            val newState = !item.isSelected
            items[position] = item.copy(isSelected = newState)
            notifyItemChanged(position)
        }
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
            CardViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryTimelineItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is HistoryTimelineItem.Card -> {
                (holder as CardViewHolder).bind(
                    item, position, isSelectionMode, searchQuery, blurTarget, frameClearDrawable,
                    onItemClick, onItemLongClick, onCheckBoxChanged, onCardClick, onOverflowClick
                )
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

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val blurCard: BlurView = itemView as BlurView
        private val cardContent: View = itemView.findViewById(R.id.cardContent)
        private val ivSelectedCheck: ImageView = itemView.findViewById(R.id.ivSelectedCheck)
        private val ivToolIcon: ImageView = itemView.findViewById(R.id.ivToolIcon)
        private val tvToolName: TextView = itemView.findViewById(R.id.tvToolName)
        private val statusChip: LinearLayout = itemView.findViewById(R.id.statusChip)
        private val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        private val tvStatusLabel: TextView = itemView.findViewById(R.id.tvStatusLabel)
        private val btnOverflow: FrameLayout = itemView.findViewById(R.id.btnOverflow)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
        private val tvTimeDuration: TextView = itemView.findViewById(R.id.tvTimeDuration)

        fun bind(
            item: HistoryTimelineItem.Card,
            position: Int,
            selectionMode: Boolean,
            searchQuery: String,
            blurTarget: BlurTarget,
            frameClearDrawable: Drawable?,
            onItemClick: (HistoryEntity) -> Unit,
            onItemLongClick: (HistoryEntity) -> Unit,
            onCheckBoxChanged: (HistoryEntity, Boolean) -> Unit,
            onCardClick: (HistoryTimelineItem.Card, Int) -> Unit,
            onOverflowClick: (HistoryEntity, View) -> Unit
        ) {
            val entity = item.entity
            val context = itemView.context

            GlassBlurHelper.setupBlur(blurCard, blurTarget, frameClearDrawable)

            val rawName = entity.toolName
            val rawCategory = "${entity.category} · ${if (entity.activityType == "EXECUTED") "Executed" else "Opened"}"
            val rawSummary = entity.resultSummary ?: "No details recorded."

            if (searchQuery.isNotEmpty()) {
                tvToolName.text = getHighlightedText(rawName, searchQuery, context)
                tvCategory.text = getHighlightedText(rawCategory, searchQuery, context)
                tvSummary.text = getHighlightedText(rawSummary, searchQuery, context)
            } else {
                tvToolName.text = rawName
                tvCategory.text = rawCategory
                tvSummary.text = rawSummary
            }

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

            // Selection mode & visual indicator highlight
            if (selectionMode) {
                ivSelectedCheck.visibility = View.VISIBLE
                btnOverflow.visibility = View.GONE
                if (item.isSelected) {
                    ivSelectedCheck.setBackgroundResource(R.drawable.bg_checkbox_checked)
                    ivSelectedCheck.setImageResource(R.drawable.ic_check)
                    ivSelectedCheck.imageTintList = ContextCompat.getColorStateList(context, R.color.bg_primary)
                    cardContent.setBackgroundColor(ContextCompat.getColor(context, R.color.accent_primary_transparent))
                } else {
                    ivSelectedCheck.setBackgroundResource(R.drawable.bg_checkbox_unchecked)
                    ivSelectedCheck.setImageDrawable(null)
                    cardContent.setBackgroundResource(R.drawable.bg_glass_card_overlay)
                }
            } else {
                ivSelectedCheck.visibility = View.GONE
                btnOverflow.visibility = View.VISIBLE
                val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                cardContent.background = typedArray.getDrawable(0)
                typedArray.recycle()
            }

            cardContent.setOnClickListener {
                if (selectionMode) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onCardClick(item, pos)
                    }
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

        private fun getHighlightedText(text: String, query: String, context: android.content.Context): SpannableString {
            val spannable = SpannableString(text)
            val lowerText = text.lowercase(Locale.getDefault())
            val lowerQuery = query.lowercase(Locale.getDefault())
            var startIndex = lowerText.indexOf(lowerQuery)

            val highlightColor = ContextCompat.getColor(context, R.color.accent_cyan)

            while (startIndex >= 0) {
                val endIndex = startIndex + query.length
                spannable.setSpan(
                    ForegroundColorSpan(highlightColor),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                startIndex = lowerText.indexOf(lowerQuery, endIndex)
            }
            return spannable
        }
    }
}
