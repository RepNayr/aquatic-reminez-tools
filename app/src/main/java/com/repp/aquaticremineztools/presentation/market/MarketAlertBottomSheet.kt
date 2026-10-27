package com.repp.aquaticremineztools.presentation.market

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.AlertType
import com.repp.aquaticremineztools.data.model.MarketAlert
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.repository.MarketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MarketAlertBottomSheet : BottomSheetDialogFragment() {

    private lateinit var spinnerAlertInstrument: Spinner
    private lateinit var spinnerAlertType: Spinner
    private lateinit var etAlertTargetValue: EditText
    private lateinit var tvAlertSummary: TextView
    private lateinit var btnCancelAlert: Button
    private lateinit var btnSaveAlert: Button

    private var availableInstruments: List<MarketInstrument> = emptyList()
    private var initialInstrumentId: String? = null
    var onAlertCreated: (() -> Unit)? = null

    companion object {
        fun newInstance(instrumentId: String? = null): MarketAlertBottomSheet {
            val sheet = MarketAlertBottomSheet()
            sheet.initialInstrumentId = instrumentId
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_market_alert_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupDataAndSpinners()
        setupListeners()
    }

    private fun bindViews(view: View) {
        spinnerAlertInstrument = view.findViewById(R.id.spinnerAlertInstrument)
        spinnerAlertType = view.findViewById(R.id.spinnerAlertType)
        etAlertTargetValue = view.findViewById(R.id.etAlertTargetValue)
        tvAlertSummary = view.findViewById(R.id.tvAlertSummary)
        btnCancelAlert = view.findViewById(R.id.btnCancelAlert)
        btnSaveAlert = view.findViewById(R.id.btnSaveAlert)
    }

    private fun setupDataAndSpinners() {
        val context = requireContext()
        val repository = MarketRepository.getInstance(context)

        viewLifecycleOwner.lifecycleScope.launch {
            val instrumentsResult = withContext(Dispatchers.IO) {
                repository.getAllMarketInstruments(forceRefresh = false)
            }
            availableInstruments = instrumentsResult.getOrNull() ?: emptyList()

            val instLabels = availableInstruments.map { "${it.symbol} - ${it.name}" }
            val instAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, instLabels)
            spinnerAlertInstrument.adapter = instAdapter

            initialInstrumentId?.let { id ->
                val index = availableInstruments.indexOfFirst { it.id == id }
                if (index != -1) {
                    spinnerAlertInstrument.setSelection(index)
                }
            }

            // Alert Types
            val typeLabels = AlertType.entries.map { it.displayName }
            val typeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, typeLabels)
            spinnerAlertType.adapter = typeAdapter

            updateSummary()
        }
    }

    private fun setupListeners() {
        btnCancelAlert.setOnClickListener { dismiss() }

        val changeListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateSummary()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerAlertInstrument.onItemSelectedListener = changeListener
        spinnerAlertType.onItemSelectedListener = changeListener

        etAlertTargetValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSummary()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSaveAlert.setOnClickListener {
            saveAlert()
        }
    }

    private fun updateSummary() {
        val selectedInstIndex = spinnerAlertInstrument.selectedItemPosition
        val targetInst = availableInstruments.getOrNull(selectedInstIndex)
        val selectedTypeIndex = spinnerAlertType.selectedItemPosition
        val alertType = AlertType.entries.getOrNull(selectedTypeIndex) ?: AlertType.PRICE_ABOVE
        val valStr = etAlertTargetValue.text.toString()
        val targetVal = valStr.toDoubleOrNull() ?: 0.0

        if (targetInst != null) {
            val summaryText = when (alertType) {
                AlertType.PRICE_ABOVE -> String.format(Locale.US, "Trigger when %s rises above %s%,.2f", targetInst.symbol, targetInst.currency, targetVal)
                AlertType.PRICE_BELOW -> String.format(Locale.US, "Trigger when %s drops below %s%,.2f", targetInst.symbol, targetInst.currency, targetVal)
                AlertType.PCT_CHANGE_24H -> String.format(Locale.US, "Trigger when %s 24h movement exceeds %.2f%%", targetInst.symbol, targetVal)
            }
            tvAlertSummary.text = summaryText
        } else {
            tvAlertSummary.text = "Select an instrument to preview rule summary"
        }
    }

    private fun saveAlert() {
        val selectedInstIndex = spinnerAlertInstrument.selectedItemPosition
        val targetInst = availableInstruments.getOrNull(selectedInstIndex)
        val selectedTypeIndex = spinnerAlertType.selectedItemPosition
        val alertType = AlertType.entries.getOrNull(selectedTypeIndex) ?: AlertType.PRICE_ABOVE
        val targetVal = etAlertTargetValue.text.toString().toDoubleOrNull() ?: 0.0

        if (targetInst == null) {
            Toast.makeText(requireContext(), "Select an instrument", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetVal <= 0.0) {
            Toast.makeText(requireContext(), "Enter a valid target threshold value (> 0)", Toast.LENGTH_SHORT).show()
            return
        }

        val newAlert = MarketAlert(
            instrumentId = targetInst.id,
            symbol = targetInst.symbol,
            name = targetInst.name,
            targetValue = targetVal,
            alertType = alertType,
            isActive = true
        )

        val repository = MarketRepository.getInstance(requireContext())
        repository.addAlert(newAlert)

        Toast.makeText(requireContext(), "Alert created for ${targetInst.symbol}", Toast.LENGTH_SHORT).show()
        onAlertCreated?.invoke()
        dismiss()
    }
}
