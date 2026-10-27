package com.repp.aquaticremineztools.presentation.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.repository.MarketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PortfolioHoldingBottomSheet : BottomSheetDialogFragment() {

    private lateinit var spinnerHoldingInstrument: Spinner
    private lateinit var etHoldingQuantity: EditText
    private lateinit var etHoldingCost: EditText
    private lateinit var spinnerHoldingCurrency: Spinner
    private lateinit var btnCancelHolding: Button
    private lateinit var btnSaveHolding: Button

    private var availableInstruments: List<MarketInstrument> = emptyList()
    private var initialInstrumentId: String? = null
    var onHoldingAdded: (() -> Unit)? = null

    companion object {
        fun newInstance(instrumentId: String? = null): PortfolioHoldingBottomSheet {
            val sheet = PortfolioHoldingBottomSheet()
            sheet.initialInstrumentId = instrumentId
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_portfolio_holding_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupDataAndSpinners()
        setupListeners()
    }

    private fun bindViews(view: View) {
        spinnerHoldingInstrument = view.findViewById(R.id.spinnerHoldingInstrument)
        etHoldingQuantity = view.findViewById(R.id.etHoldingQuantity)
        etHoldingCost = view.findViewById(R.id.etHoldingCost)
        spinnerHoldingCurrency = view.findViewById(R.id.spinnerHoldingCurrency)
        btnCancelHolding = view.findViewById(R.id.btnCancelHolding)
        btnSaveHolding = view.findViewById(R.id.btnSaveHolding)
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
            spinnerHoldingInstrument.adapter = instAdapter

            initialInstrumentId?.let { id ->
                val index = availableInstruments.indexOfFirst { it.id == id }
                if (index != -1) {
                    spinnerHoldingInstrument.setSelection(index)
                }
            }

            val currencies = listOf("USD", "IDR")
            val currAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, currencies)
            spinnerHoldingCurrency.adapter = currAdapter
        }
    }

    private fun setupListeners() {
        btnCancelHolding.setOnClickListener { dismiss() }

        spinnerHoldingInstrument.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val inst = availableInstruments.getOrNull(position)
                if (inst != null) {
                    etHoldingCost.setText(inst.currentPrice?.toString() ?: "0.0")
                    if (inst.currency == "IDR") {
                        spinnerHoldingCurrency.setSelection(1) // IDR
                    } else {
                        spinnerHoldingCurrency.setSelection(0) // USD
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSaveHolding.setOnClickListener {
            saveHolding()
        }
    }

    private fun saveHolding() {
        val selectedInstIndex = spinnerHoldingInstrument.selectedItemPosition
        val targetInst = availableInstruments.getOrNull(selectedInstIndex)
        val qty = etHoldingQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val cost = etHoldingCost.text.toString().toDoubleOrNull() ?: 0.0
        val currency = spinnerHoldingCurrency.selectedItem?.toString() ?: "USD"

        if (targetInst == null) {
            Toast.makeText(requireContext(), "Select an instrument", Toast.LENGTH_SHORT).show()
            return
        }

        if (qty <= 0.0) {
            Toast.makeText(requireContext(), "Quantity must be greater than zero", Toast.LENGTH_SHORT).show()
            return
        }

        if (cost < 0.0) {
            Toast.makeText(requireContext(), "Cost basis cannot be negative", Toast.LENGTH_SHORT).show()
            return
        }

        val newHolding = PortfolioHolding(
            instrumentId = targetInst.id,
            symbol = targetInst.symbol,
            name = targetInst.name,
            quantity = qty,
            averageCost = cost,
            currency = currency
        )

        val repository = MarketRepository.getInstance(requireContext())
        repository.addPortfolioHolding(newHolding)

        Toast.makeText(requireContext(), "Added ${targetInst.symbol} to Portfolio", Toast.LENGTH_SHORT).show()
        onHoldingAdded?.invoke()
        dismiss()
    }
}
