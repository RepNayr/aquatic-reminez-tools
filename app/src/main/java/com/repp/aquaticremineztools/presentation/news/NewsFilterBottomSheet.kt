package com.repp.aquaticremineztools.presentation.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.repp.aquaticremineztools.R

class NewsFilterBottomSheet : BottomSheetDialogFragment() {

    var onApply: ((String) -> Unit)? = null
    var onReset: (() -> Unit)? = null
    private var selectedCategory: String = "All"

    companion object {
        private const val ARG_CATEGORY = "arg_category"

        fun newInstance(
            currentCategory: String,
            onApply: ((String) -> Unit)? = null,
            onReset: (() -> Unit)? = null
        ): NewsFilterBottomSheet {
            val fragment = NewsFilterBottomSheet()
            val args = Bundle().apply {
                putString(ARG_CATEGORY, currentCategory)
            }
            fragment.arguments = args
            fragment.onApply = onApply
            fragment.onReset = onReset
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCategory = arguments?.getString(ARG_CATEGORY) ?: "All"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_news_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupNewsCategory)
        val btnApply = view.findViewById<MaterialButton>(R.id.btnApplyFilter)
        val btnReset = view.findViewById<MaterialButton>(R.id.btnResetFilter)

        // Select initial chip
        var pendingCategory = selectedCategory
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.text.toString().equals(selectedCategory, ignoreCase = true)) {
                chip.isChecked = true
                break
            }
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                if (chip != null) {
                    pendingCategory = chip.text.toString()
                }
            } else {
                pendingCategory = "All"
            }
        }

        btnApply.setOnClickListener {
            selectedCategory = pendingCategory
            onApply?.invoke(selectedCategory)
            dismiss()
        }

        btnReset.setOnClickListener {
            pendingCategory = "All"
            onReset?.invoke()
            dismiss()
        }
    }
}
