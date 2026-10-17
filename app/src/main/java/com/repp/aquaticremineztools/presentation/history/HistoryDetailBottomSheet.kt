package com.repp.aquaticremineztools.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.ToolIconRegistry
import com.repp.aquaticremineztools.data.local.history.HistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDetailBottomSheet(
    private val entity: HistoryEntity,
    private val onDelete: (HistoryEntity) -> Unit,
    private val onOpenTool: (HistoryEntity) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_history_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivDetailIcon: ImageView = view.findViewById(R.id.ivDetailIcon)
        val tvDetailToolName: TextView = view.findViewById(R.id.tvDetailToolName)
        val tvDetailCategory: TextView = view.findViewById(R.id.tvDetailCategory)
        val detailStatusChip: LinearLayout = view.findViewById(R.id.detailStatusChip)
        val ivDetailStatusIcon: ImageView = view.findViewById(R.id.ivDetailStatusIcon)
        val tvDetailStatusLabel: TextView = view.findViewById(R.id.tvDetailStatusLabel)
        val tvDetailType: TextView = view.findViewById(R.id.tvDetailType)
        val tvDetailDuration: TextView = view.findViewById(R.id.tvDetailDuration)
        val tvDetailTimestamp: TextView = view.findViewById(R.id.tvDetailTimestamp)
        val tvDetailSummary: TextView = view.findViewById(R.id.tvDetailSummary)
        val btnDeleteDetail: MaterialButton = view.findViewById(R.id.btnDeleteDetail)
        val btnOpenTool: MaterialButton = view.findViewById(R.id.btnOpenTool)

        tvDetailToolName.text = entity.toolName
        tvDetailCategory.text = entity.category
        tvDetailType.text = if (entity.activityType == "EXECUTED") "Executed" else "Opened"

        val durationStr = if (entity.durationMs != null && entity.durationMs > 0) {
            String.format(Locale.getDefault(), "%.1f sec", entity.durationMs / 1000.0)
        } else "N/A"
        tvDetailDuration.text = durationStr

        val fullDateStr = SimpleDateFormat("EEEE, MMMM d, yyyy · HH:mm:ss", Locale.getDefault()).format(Date(entity.timestamp))
        tvDetailTimestamp.text = fullDateStr
        tvDetailSummary.text = entity.resultSummary ?: "No summary or logs recorded."

        val context = requireContext()
        // Tool Icon mapped by toolId
        val iconRes = ToolIconRegistry.getIconRes(entity.toolId)
        ivDetailIcon.setImageResource(iconRes)
        ivDetailIcon.imageTintList = null
        ivDetailIcon.clearColorFilter()

        if (entity.activityType == "OPENED") {
            detailStatusChip.visibility = View.VISIBLE
            ivDetailStatusIcon.setImageResource(R.drawable.ic_info)
            ivDetailStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            tvDetailStatusLabel.text = "Opened"
            tvDetailStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        } else {
            detailStatusChip.visibility = View.VISIBLE
            when (entity.status) {
                "SUCCESS" -> {
                    ivDetailStatusIcon.setImageResource(R.drawable.ic_status_success)
                    ivDetailStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_online))
                    tvDetailStatusLabel.text = "Success"
                    tvDetailStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.status_online))
                }
                "FAILED" -> {
                    ivDetailStatusIcon.setImageResource(R.drawable.ic_status_failed)
                    ivDetailStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_error))
                    tvDetailStatusLabel.text = "Failed"
                    tvDetailStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.status_error))
                }
                "RUNNING" -> {
                    ivDetailStatusIcon.setImageResource(R.drawable.ic_status_running)
                    ivDetailStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_blue))
                    tvDetailStatusLabel.text = "Running"
                    tvDetailStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
                }
                "CANCELLED" -> {
                    ivDetailStatusIcon.setImageResource(R.drawable.ic_status_cancel)
                    ivDetailStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_warning))
                    tvDetailStatusLabel.text = "Cancelled"
                    tvDetailStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                }
                "INTERRUPTED" -> {
                    ivDetailStatusIcon.setImageResource(R.drawable.ic_status_interrupted)
                    ivDetailStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_violet))
                    tvDetailStatusLabel.text = "Interrupted"
                    tvDetailStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.accent_violet))
                }
                else -> {
                    detailStatusChip.visibility = View.GONE
                }
            }
        }

        if (entity.status == "RUNNING") {
            btnDeleteDetail.isEnabled = false
            btnDeleteDetail.alpha = 0.5f
        }

        btnDeleteDetail.setOnClickListener {
            if (entity.status == "RUNNING") {
                Toast.makeText(context, "Cannot delete ongoing execution", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onDelete(entity)
            dismiss()
        }

        btnOpenTool.setOnClickListener {
            onOpenTool(entity)
            dismiss()
        }
    }
}
