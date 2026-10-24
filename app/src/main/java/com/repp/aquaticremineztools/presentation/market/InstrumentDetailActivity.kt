package com.repp.aquaticremineztools.presentation.market

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.repository.MarketRepository
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class InstrumentDetailActivity : AppCompatActivity() {

    private lateinit var tvDetailSymbol: TextView
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailCurrentPrice: TextView
    private lateinit var tvDetailChange: TextView
    private lateinit var tvDetailSourceStatus: TextView
    private lateinit var chartInstrument: LineChart
    private lateinit var btnWatchlistToggle: ImageView
    private lateinit var btnAddPortfolio: Button
    private lateinit var btnToggleIndicator: TextView
    private lateinit var btnBack: ImageView

    private var instrumentId: String = ""
    private var currentInstrument: MarketInstrument? = null
    private var isSmaActive: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_instrument_detail)

        instrumentId = intent.getStringExtra("instrument_id") ?: "bitcoin"

        bindViews()
        setupListeners()
        loadInstrumentData()
    }

    private fun bindViews() {
        tvDetailSymbol = findViewById(R.id.tvDetailSymbol)
        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailCurrentPrice = findViewById(R.id.tvDetailCurrentPrice)
        tvDetailChange = findViewById(R.id.tvDetailChange)
        tvDetailSourceStatus = findViewById(R.id.tvDetailSourceStatus)
        chartInstrument = findViewById(R.id.chartInstrument)
        btnWatchlistToggle = findViewById(R.id.btnWatchlistToggle)
        btnAddPortfolio = findViewById(R.id.btnAddPortfolio)
        btnToggleIndicator = findViewById(R.id.btnToggleIndicator)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnWatchlistToggle.setOnClickListener {
            currentInstrument?.let { inst ->
                val repository = MarketRepository.getInstance(this)
                val added = repository.toggleWatchlist(inst)
                updateWatchlistIcon(added)
                Toast.makeText(this, if (added) "Added to Watchlist" else "Removed from Watchlist", Toast.LENGTH_SHORT).show()
            }
        }

        btnToggleIndicator.setOnClickListener {
            isSmaActive = !isSmaActive
            btnToggleIndicator.text = if (isSmaActive) "SMA: ON" else "SMA: OFF"
            currentInstrument?.let { renderChart(it) }
        }

        btnAddPortfolio.setOnClickListener {
            currentInstrument?.let { showAddPortfolioDialog(it) }
        }
    }

    private fun loadInstrumentData() {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = MarketRepository.getInstance(this@InstrumentDetailActivity)
            val result = repository.getAllMarketInstruments(forceRefresh = false)
            result.onSuccess { list ->
                val found = list.find { it.id == instrumentId }
                withContext(Dispatchers.Main) {
                    if (found != null) {
                        currentInstrument = found
                        populateDetails(found)
                        renderChart(found)
                        updateWatchlistIcon(repository.isInWatchlist(found.id))
                    }
                }
            }
        }
    }

    private fun populateDetails(instrument: MarketInstrument) {
        tvDetailSymbol.text = instrument.symbol
        tvDetailName.text = instrument.name

        if (instrument.currentPrice != null) {
            val formatted = if (instrument.currency == "IDR") {
                String.format(Locale.US, "Rp %,.2f", instrument.currentPrice)
            } else if (instrument.currentPrice >= 1.0) {
                String.format(Locale.US, "$%,.2f", instrument.currentPrice)
            } else {
                String.format(Locale.US, "$%,.4f", instrument.currentPrice)
            }
            tvDetailCurrentPrice.text = formatted
        } else {
            tvDetailCurrentPrice.text = "--"
        }

        if (instrument.priceChangePercentage24h != null) {
            val isPos = instrument.priceChangePercentage24h >= 0
            val sign = if (isPos) "+" else ""
            tvDetailChange.text = String.format(Locale.US, "%s%.2f%%", sign, instrument.priceChangePercentage24h)
            val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
            tvDetailChange.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            tvDetailChange.text = "--"
        }

        tvDetailSourceStatus.text = if (instrument.isDemo) "Demo Data" else if (instrument.isCached) "Cached Data" else "Live · Market Feed"
    }

    private fun renderChart(instrument: MarketInstrument) {
        val candles = instrument.historicalCandles
        if (candles.isNullOrEmpty()) {
            chartInstrument.clear()
            return
        }

        val entries = ArrayList<Entry>()
        val smaEntries = ArrayList<Entry>()
        val window = 5

        candles.forEachIndexed { index, candle ->
            entries.add(Entry(index.toFloat(), candle.close))
            if (isSmaActive && (index >= window - 1)) {
                val slice = candles.subList(index - window + 1, index + 1)
                val avg = slice.map { it.close }.average().toFloat()
                smaEntries.add(Entry(index.toFloat(), avg))
            }
        }

        val dataSet = LineDataSet(entries, "Close Price").apply {
            color = Color.parseColor("#43D9E6")
            setDrawValues(false)
            lineWidth = 2f
            setDrawCircles(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#43D9E6")
            fillAlpha = 40
        }

        val dataSets = ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
        dataSets.add(dataSet)

        if (isSmaActive && smaEntries.isNotEmpty()) {
            val smaDataSet = LineDataSet(smaEntries, "SMA 5").apply {
                color = Color.parseColor("#38BFA1")
                setDrawValues(false)
                lineWidth = 1.5f
                setDrawCircles(false)
            }
            dataSets.add(smaDataSet)
        }

        chartInstrument.data = LineData(dataSets)
        chartInstrument.description.isEnabled = false
        chartInstrument.legend.isEnabled = true
        chartInstrument.legend.textColor = Color.WHITE
        chartInstrument.xAxis.isEnabled = false
        chartInstrument.axisRight.isEnabled = false
        chartInstrument.axisLeft.textColor = Color.WHITE
        chartInstrument.invalidate()
    }

    private fun updateWatchlistIcon(isInWatchlist: Boolean) {
        val tintColor = if (isInWatchlist) R.color.accent_teal else R.color.accent_primary
        btnWatchlistToggle.imageTintList = ContextCompat.getColorStateList(this, tintColor)
    }

    private fun showAddPortfolioDialog(instrument: MarketInstrument) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_holding, null)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etHoldingQuantity)
        val etCost = dialogView.findViewById<EditText>(R.id.etHoldingCost)

        etCost.setText(instrument.currentPrice?.toString() ?: "0.0")

        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Add to Portfolio")
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
                        currency = instrument.currency,
                    )
                    MarketRepository.getInstance(this).addPortfolioHolding(holding)
                    Toast.makeText(this, "Holding added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid quantity or cost", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
