package com.repp.aquaticremineztools.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.repp.aquaticremineztools.R

data class HistoryFilterOptions(
    val category: String? = null,
    val activityType: String? = null,
    val status: String? = null,
    val dateRange: String? = null
)

class HistoryFilterBottomSheet : BottomSheetDialogFragment() {

    var onApply: ((HistoryFilterOptions) -> Unit)? = null
    var onReset: (() -> Unit)? = null
    private var currentFilters = HistoryFilterOptions()

    companion object {
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_DATE = "arg_date"

        fun newInstance(
            currentFilters: HistoryFilterOptions,
            onApply: ((HistoryFilterOptions) -> Unit)? = null,
            onReset: (() -> Unit)? = null
        ): HistoryFilterBottomSheet {
            val fragment = HistoryFilterBottomSheet()
            val args = Bundle().apply {
                putString(ARG_CATEGORY, currentFilters.category)
                putString(ARG_TYPE, currentFilters.activityType)
                putString(ARG_STATUS, currentFilters.status)
                putString(ARG_DATE, currentFilters.dateRange)
            }
            fragment.arguments = args
            fragment.onApply = onApply
            fragment.onReset = onReset
            return fragment
        }
    }

    private val categories = listOf("Utility", "Security & Generate", "File & System", "Cyber Tools", "Media", "Dev Tools", "Forensic", "Security", "Games")
    private val types = listOf("OPENED", "EXECUTED")
    private val statuses = listOf("SUCCESS", "FAILED", "RUNNING", "CANCELLED", "INTERRUPTED")
    private val dateRanges = listOf("TODAY" to "Today", "LAST_7" to "Last 7 days", "LAST_30" to "Last 30 days")

    private lateinit var cgCategory: ChipGroup
    private lateinit var cgType: ChipGroup
    private lateinit var cgStatus: ChipGroup
    private lateinit var cgDate: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            currentFilters = HistoryFilterOptions(
                category = args.getString(ARG_CATEGORY),
                activityType = args.getString(ARG_TYPE),
                status = args.getString(ARG_STATUS),
                dateRange = args.getString(ARG_DATE)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_history_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cgCategory = view.findViewById(R.id.cgCategory)
        cgType = view.findViewById(R.id.cgType)
        cgStatus = view.findViewById(R.id.cgStatus)
        cgDate = view.findViewById(R.id.cgDate)

        val btnResetFilter: MaterialButton = view.findViewById(R.id.btnResetFilter)
        val btnApplyFilter: MaterialButton = view.findViewById(R.id.btnApplyFilter)

        populateChips()

        btnResetFilter.setOnClickListener {
            onReset?.invoke()
            dismiss()
        }

        btnApplyFilter.setOnClickListener {
            val selectedCatId = cgCategory.checkedChipId
            val selectedTypeId = cgType.checkedChipId
            val selectedStatusId = cgStatus.checkedChipId
            val selectedDateId = cgDate.checkedChipId

            val cat = if (selectedCatId != View.NO_ID) view.findViewById<Chip>(selectedCatId)?.tag as? String else null
            val type = if (selectedTypeId != View.NO_ID) view.findViewById<Chip>(selectedTypeId)?.tag as? String else null
            val status = if (selectedStatusId != View.NO_ID) view.findViewById<Chip>(selectedStatusId)?.tag as? String else null
            val date = if (selectedDateId != View.NO_ID) view.findViewById<Chip>(selectedDateId)?.tag as? String else null

            onApply?.invoke(HistoryFilterOptions(cat, type, status, date))
            dismiss()
        }
    }

    private fun populateChips() {
        val context = requireContext()

        categories.forEach { cat ->
            val chip = Chip(context).apply {
                text = cat
                tag = cat
                isCheckable = true
                isChecked = (currentFilters.category == cat)
                setChipBackgroundColorResource(R.color.bg_card_elevated)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.border)
                chipStrokeWidth = 1f
                checkedIconTint = ContextCompat.getColorStateList(context, R.color.accent_primary)
            }
            cgCategory.addView(chip)
        }

        types.forEach { type ->
            val label = if (type == "EXECUTED") "Executed" else "Opened"
            val chip = Chip(context).apply {
                text = label
                tag = type
                isCheckable = true
                isChecked = (currentFilters.activityType == type)
                setChipBackgroundColorResource(R.color.bg_card_elevated)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.border)
                chipStrokeWidth = 1f
                checkedIconTint = ContextCompat.getColorStateList(context, R.color.accent_primary)
            }
            cgType.addView(chip)
        }

        statuses.forEach { st ->
            val label = st.lowercase().replaceFirstChar { it.uppercase() }
            val chip = Chip(context).apply {
                text = label
                tag = st
                isCheckable = true
                isChecked = (currentFilters.status == st)
                setChipBackgroundColorResource(R.color.bg_card_elevated)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.border)
                chipStrokeWidth = 1f
                checkedIconTint = ContextCompat.getColorStateList(context, R.color.accent_primary)
            }
            cgStatus.addView(chip)
        }

        dateRanges.forEach { (key, label) ->
            val chip = Chip(context).apply {
                text = label
                tag = key
                isCheckable = true
                isChecked = (currentFilters.dateRange == key)
                setChipBackgroundColorResource(R.color.bg_card_elevated)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.border)
                chipStrokeWidth = 1f
                checkedIconTint = ContextCompat.getColorStateList(context, R.color.accent_primary)
            }
            cgDate.addView(chip)
        }
    }
}