package com.repp.aquaticremineztools.presentation.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.repository.MarketRepository
import com.repp.aquaticremineztools.presentation.home.SparklineView
import java.util.Locale

class MarketInstrumentBottomSheet : BottomSheetDialogFragment() {

    private lateinit var ivBsLogo: ImageView
    private lateinit var tvBsFallback: TextView
    private lateinit var tvBsSymbol: TextView
    private lateinit var tvBsExchange: TextView
    private lateinit var btnBsClose: ImageView
    private lateinit var tvBsPrice: TextView
    private lateinit var tvBsChange: TextView
    private lateinit var tvBsDataStatus: TextView
    private lateinit var sparklineBs: SparklineView
    private lateinit var tvBsNoChart: TextView
    private lateinit var btnBsWatchlist: Button
    private lateinit var btnBsPortfolio: Button
    private lateinit var btnBsFullDetails: Button

    private var currentInstrument: MarketInstrument? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_instrument_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupListeners()
        populateData()
    }

    fun setInstrument(instrument: MarketInstrument) {
        currentInstrument = instrument
        if (isAdded) {
            populateData()
        }
    }

    private fun bindViews(view: View) {
        ivBsLogo = view.findViewById(R.id.ivBsLogo)
        tvBsFallback = view.findViewById(R.id.tvBsFallback)
        tvBsSymbol = view.findViewById(R.id.tvBsSymbol)
        tvBsExchange = view.findViewById(R.id.tvBsExchange)
        btnBsClose = view.findViewById(R.id.btnBsClose)
        tvBsPrice = view.findViewById(R.id.tvBsPrice)
        tvBsChange = view.findViewById(R.id.tvBsChange)
        tvBsDataStatus = view.findViewById(R.id.tvBsDataStatus)
        sparklineBs = view.findViewById(R.id.sparklineBs)
        tvBsNoChart = view.findViewById(R.id.tvBsNoChart)
        btnBsWatchlist = view.findViewById(R.id.btnBsWatchlist)
        btnBsPortfolio = view.findViewById(R.id.btnBsPortfolio)
        btnBsFullDetails = view.findViewById(R.id.btnBsFullDetails)
        btnBsFullDetails.visibility = View.GONE
    }

    private fun setupListeners() {
        btnBsClose.setOnClickListener { dismiss() }

        btnBsWatchlist.setOnClickListener {
            val inst = currentInstrument ?: return@setOnClickListener
            val context = requireContext()
            val repo = MarketRepository.getInstance(context)
            val added = repo.toggleWatchlist(inst)
            updateWatchlistButton(added)
            Toast.makeText(context, if (added) "Saved to Watchlist" else "Removed from Watchlist", Toast.LENGTH_SHORT).show()
        }

        btnBsPortfolio.setOnClickListener {
            val inst = currentInstrument ?: return@setOnClickListener
            showAddPortfolioDialog(inst)
        }
    }

    private fun populateData() {
        val inst = currentInstrument ?: return
        val context = requireContext()
        val repo = MarketRepository.getInstance(context)

        tvBsSymbol.text = "${inst.symbol} · ${inst.name}"
        tvBsExchange.text = "${inst.exchange} · ${inst.category.displayName}"

        // Logo
        if (!inst.imageUrl.isNullOrEmpty()) {
            tvBsFallback.visibility = View.GONE
            ivBsLogo.visibility = View.VISIBLE
            ivBsLogo.load(inst.imageUrl) {
                crossfade(true)
                listener(
                    onError = { _, _ ->
                        ivBsLogo.visibility = View.GONE
                        tvBsFallback.visibility = View.VISIBLE
                        tvBsFallback.text = inst.symbol.take(3)
                    }
                )
            }
        } else {
            ivBsLogo.visibility = View.GONE
            tvBsFallback.visibility = View.VISIBLE
            tvBsFallback.text = inst.symbol.take(3)
        }

        // Price & Change
        val price = inst.currentPrice
        if (price != null) {
            val formatted = when (inst.currency) {
                "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                "%" -> String.format(Locale.US, "%.2f%%", price)
                "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                "EUR" -> String.format(Locale.US, "€%,.4f", price)
                else -> if (price >= 1.0) String.format(Locale.US, "$%,.2f", price) else String.format(Locale.US, "$%,.4f", price)
            }
            tvBsPrice.text = formatted
        } else {
            tvBsPrice.text = "--"
        }

        val pctChange = inst.priceChangePercentage24h
        if (pctChange != null) {
            val isPos = pctChange >= 0
            val sign = if (isPos) "+" else ""
            tvBsChange.text = String.format(Locale.US, "%s%.2f%% (24h)", sign, pctChange)
            val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
            tvBsChange.setTextColor(ContextCompat.getColor(context, colorRes))
        } else {
            tvBsChange.text = "Feed Unavailable"
            tvBsChange.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        }

        // Data Status Badge
        tvBsDataStatus.text = when (inst.dataStatus) {
            MarketDataStatus.LIVE -> "LIVE FEED"
            MarketDataStatus.CACHED -> "OFFLINE CACHE"
            MarketDataStatus.SIMULATED -> "DEMO DATA"
            MarketDataStatus.UNAVAILABLE -> "FEED UNAVAILABLE"
        }

        // Sparkline
        val sparkline = inst.sparklinePrices
        if (!sparkline.isNullOrEmpty()) {
            sparklineBs.visibility = View.VISIBLE
            tvBsNoChart.visibility = View.GONE
            val isPos = (pctChange ?: 0.0) >= 0
            sparklineBs.setSparklineData(sparkline, isPos)
        } else {
            sparklineBs.visibility = View.GONE
            tvBsNoChart.visibility = View.VISIBLE
        }

        updateWatchlistButton(repo.isInWatchlist(inst.id))
    }

    private fun updateWatchlistButton(isInWatchlist: Boolean) {
        if (!isAdded) return
        val context = requireContext()
        if (isInWatchlist) {
            btnBsWatchlist.text = "Saved ✓"
            btnBsWatchlist.setTextColor(ContextCompat.getColor(context, R.color.accent_teal))
        } else {
            btnBsWatchlist.text = "+ Watchlist"
            btnBsWatchlist.setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
        }
    }

    private fun showAddPortfolioDialog(instrument: MarketInstrument) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_holding, null)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etHoldingQuantity)
        val etCost = dialogView.findViewById<EditText>(R.id.etHoldingCost)

        etCost.setText(instrument.currentPrice?.toString() ?: "0.0")

        AlertDialog.Builder(context, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Add ${instrument.symbol} to Portfolio")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                val cost = etCost.text.toString().toDoubleOrNull() ?: 0.0
                if (qty > 0 && cost >= 0) {
                    val holding = PortfolioHolding(
                        instrumentId = instrument.id,
                        symbol = instrument.symbol,
                        name = instrument.name,
                        quantity = qty,
                        averageCost = cost,
                        currency = instrument.currency
                    )
                    MarketRepository.getInstance(context).addPortfolioHolding(holding)
                    Toast.makeText(context, "Added to Portfolio", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid quantity or cost", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        fun newInstance(instrument: MarketInstrument): MarketInstrumentBottomSheet {
            val fragment = MarketInstrumentBottomSheet()
            fragment.setInstrument(instrument)
            return fragment
        }
    }
}
