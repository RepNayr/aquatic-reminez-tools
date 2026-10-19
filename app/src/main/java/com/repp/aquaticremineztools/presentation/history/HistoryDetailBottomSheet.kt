package com.repp.aquaticremineztools.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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

class HistoryDetailBottomSheet : BottomSheetDialogFragment() {

    private var entity: HistoryEntity? = null
    var onDelete: ((HistoryEntity) -> Unit)? = null
    var onOpenTool: ((HistoryEntity) -> Unit)? = null

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_TOOL_ID = "arg_tool_id"
        private const val ARG_TOOL_NAME = "arg_tool_name"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_ACTIVITY_TYPE = "arg_activity_type"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_TIMESTAMP = "arg_timestamp"
        private const val ARG_END_TIME = "arg_end_time"
        private const val ARG_DURATION_MS = "arg_duration_ms"
        private const val ARG_RESULT_SUMMARY = "arg_result_summary"

        fun newInstance(
            entity: HistoryEntity,
            onDelete: ((HistoryEntity) -> Unit)? = null,
            onOpenTool: ((HistoryEntity) -> Unit)? = null
        ): HistoryDetailBottomSheet {
            val fragment = HistoryDetailBottomSheet()
            val args = Bundle().apply {
                putLong(ARG_ID, entity.id)
                putString(ARG_TOOL_ID, entity.toolId)
                putString(ARG_TOOL_NAME, entity.toolName)
                putString(ARG_CATEGORY, entity.category)
                putString(ARG_ACTIVITY_TYPE, entity.activityType)
                putString(ARG_STATUS, entity.status)
                putLong(ARG_TIMESTAMP, entity.timestamp)
                entity.endTime?.let { putLong(ARG_END_TIME, it) }
                entity.durationMs?.let { putLong(ARG_DURATION_MS, it) }
                putString(ARG_RESULT_SUMMARY, entity.resultSummary)
            }
            fragment.arguments = args
            fragment.onDelete = onDelete
            fragment.onOpenTool = onOpenTool
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            val id = args.getLong(ARG_ID, 0L)
            val toolId = args.getString(ARG_TOOL_ID) ?: ""
            val toolName = args.getString(ARG_TOOL_NAME) ?: ""
            val category = args.getString(ARG_CATEGORY) ?: ""
            val activityType = args.getString(ARG_ACTIVITY_TYPE) ?: "OPENED"
            val status = args.getString(ARG_STATUS)
            val timestamp = args.getLong(ARG_TIMESTAMP, System.currentTimeMillis())
            val endTime = if (args.containsKey(ARG_END_TIME)) args.getLong(ARG_END_TIME) else null
            val durationMs = if (args.containsKey(ARG_DURATION_MS)) args.getLong(ARG_DURATION_MS) else null
            val resultSummary = args.getString(ARG_RESULT_SUMMARY)

            entity = HistoryEntity(
                id = id,
                toolId = toolId,
                toolName = toolName,
                category = category,
                activityType = activityType,
                status = status,
                timestamp = timestamp,
                endTime = endTime,
                durationMs = durationMs,
                resultSummary = resultSummary
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_history_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ent = entity ?: return

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

        tvDetailToolName.text = ent.toolName
        tvDetailCategory.text = ent.category
        tvDetailType.text = if (ent.activityType == "EXECUTED") "Executed" else "Opened"

        val durationStr = if (ent.durationMs != null && ent.durationMs > 0) {
            String.format(Locale.getDefault(), "%.1f sec", ent.durationMs / 1000.0)
        } else "N/A"
        tvDetailDuration.text = durationStr

        val fullDateStr = SimpleDateFormat("EEEE, MMMM d, yyyy · HH:mm:ss", Locale.getDefault()).format(Date(ent.timestamp))
        tvDetailTimestamp.text = fullDateStr
        tvDetailSummary.text = ent.resultSummary ?: "No summary or logs recorded."

        val context = requireContext()
        val iconRes = ToolIconRegistry.getIconRes(ent.toolId)
        ivDetailIcon.setImageResource(iconRes)
        ivDetailIcon.imageTintList = null
        ivDetailIcon.clearColorFilter()

        if (ent.activityType == "OPENED") {
            detailStatusChip.visibility = View.VISIBLE
            ivDetailStatusIcon.setImageResource(R.drawable.ic_info)
            ivDetailStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            tvDetailStatusLabel.text = "Opened"
            tvDetailStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        } else {
            detailStatusChip.visibility = View.VISIBLE
            when (ent.status) {
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

        if (ent.status == "RUNNING") {
            btnDeleteDetail.isEnabled = false
            btnDeleteDetail.alpha = 0.5f
        }

        btnDeleteDetail.setOnClickListener {
            if (ent.status == "RUNNING") {
                Toast.makeText(context, "Cannot delete ongoing execution", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onDelete?.invoke(ent)
            dismissAllowingStateLoss()
        }

        btnOpenTool.setOnClickListener {
            onOpenTool?.invoke(ent)
            dismissAllowingStateLoss()
        }
    }
}