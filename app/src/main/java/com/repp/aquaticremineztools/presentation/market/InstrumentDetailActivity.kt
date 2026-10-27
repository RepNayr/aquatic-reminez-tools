package com.repp.aquaticremineztools.presentation.market

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.repository.MarketRepository
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
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
    private lateinit var btnCreateAlert: ImageView
    private lateinit var btnAddPortfolio: Button
    private lateinit var btnToggleIndicator: TextView
    private lateinit var btnBack: ImageView

    private lateinit var tvStatHigh: TextView
    private lateinit var tvStatLow: TextView
    private lateinit var tvStatCategory: TextView
    private lateinit var tvStatExchange: TextView

    private lateinit var btnTf1d: TextView
    private lateinit var btnTf1w: TextView
    private lateinit var btnTf1m: TextView
    private lateinit var btnTf3m: TextView
    private lateinit var btnTfYtd: TextView
    private lateinit var btnTfAll: TextView

    private var instrumentId: String = "bitcoin"
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

        val extraId = intent.getStringExtra("instrument_id")
        if (!extraId.isNullOrEmpty()) {
            instrumentId = extraId
        }

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
        btnCreateAlert = findViewById(R.id.btnCreateAlert)
        btnAddPortfolio = findViewById(R.id.btnAddPortfolio)
        btnToggleIndicator = findViewById(R.id.btnToggleIndicator)
        btnBack = findViewById(R.id.btnBack)

        tvStatHigh = findViewById(R.id.tvStatHigh)
        tvStatLow = findViewById(R.id.tvStatLow)
        tvStatCategory = findViewById(R.id.tvStatCategory)
        tvStatExchange = findViewById(R.id.tvStatExchange)

        btnTf1d = findViewById(R.id.btnTf1d)
        btnTf1w = findViewById(R.id.btnTf1w)
        btnTf1m = findViewById(R.id.btnTf1m)
        btnTf3m = findViewById(R.id.btnTf3m)
        btnTfYtd = findViewById(R.id.btnTfYtd)
        btnTfAll = findViewById(R.id.btnTfAll)
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

        btnCreateAlert.setOnClickListener {
            currentInstrument?.let { inst ->
                val sheet = MarketAlertBottomSheet.newInstance(inst.id)
                sheet.show(supportFragmentManager, "MarketAlertBottomSheet")
            }
        }

        btnToggleIndicator.setOnClickListener {
            isSmaActive = !isSmaActive
            btnToggleIndicator.text = if (isSmaActive) "SMA: ON" else "SMA: OFF"
            currentInstrument?.let { renderChart(it) }
        }

        btnAddPortfolio.setOnClickListener {
            currentInstrument?.let { inst ->
                val sheet = PortfolioHoldingBottomSheet.newInstance(inst.id)
                sheet.show(supportFragmentManager, "PortfolioHoldingBottomSheet")
            }
        }

        val rangeBtns = listOf(btnTf1d to "1D", btnTf1w to "1W", btnTf1m to "1M", btnTf3m to "3M", btnTfYtd to "1Y", btnTfAll to "ALL")
        rangeBtns.forEach { (btn, _) ->
            btn.setOnClickListener {
                rangeBtns.forEach { (b, _) ->
                    b.setBackgroundResource(R.drawable.bg_chip_inactive)
                    b.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
                btn.setBackgroundResource(R.drawable.bg_chip_cyan_active)
                btn.setTextColor(ContextCompat.getColor(this, R.color.accent_cyan))
            }
        }
    }

    private fun loadInstrumentData() {
        lifecycleScope.launch(Dispatchers.IO) {
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
        tvDetailName.text = "${instrument.name} · ${instrument.exchange}"

        val price = instrument.currentPrice
        if (price != null) {
            val formatted = when (instrument.currency) {
                "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                "%" -> String.format(Locale.US, "%.2f%%", price)
                "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                "EUR" -> String.format(Locale.US, "€%,.4f", price)
                else -> if (price >= 1.0) {
                    String.format(Locale.US, "$%,.2f", price)
                } else {
                    String.format(Locale.US, "$%,.4f", price)
                }
            }
            tvDetailCurrentPrice.text = formatted
        } else {
            tvDetailCurrentPrice.text = "--"
        }

        val pctChange = instrument.priceChangePercentage24h
        if (pctChange != null) {
            val isPos = pctChange >= 0
            val sign = if (isPos) "+" else ""
            tvDetailChange.text = String.format(Locale.US, "%s%.2f%% (24h)", sign, pctChange)
            val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
            tvDetailChange.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            tvDetailChange.text = "--"
        }

        tvDetailSourceStatus.text = when (instrument.dataStatus) {
            MarketDataStatus.LIVE -> "LIVE FEED"
            MarketDataStatus.CACHED -> "CACHED"
            MarketDataStatus.SIMULATED -> "SIMULATED"
            MarketDataStatus.UNAVAILABLE -> "UNAVAILABLE"
        }

        // Key Statistics
        val sparkline = instrument.sparklinePrices
        val high = if (!sparkline.isNullOrEmpty()) sparkline.maxOrNull()?.toDouble() else price?.let { it * 1.03 }
        val low = if (!sparkline.isNullOrEmpty()) sparkline.minOrNull()?.toDouble() else price?.let { it * 0.97 }

        tvStatHigh.text = high?.let { formatPriceValue(it, instrument.currency) } ?: "--"
        tvStatLow.text = low?.let { formatPriceValue(it, instrument.currency) } ?: "--"
        tvStatCategory.text = instrument.category.displayName
        tvStatExchange.text = instrument.exchange
    }

    private fun formatPriceValue(value: Double, currency: String): String {
        return when (currency) {
            "IDR" -> String.format(Locale.US, "Rp %,.2f", value)
            "%" -> String.format(Locale.US, "%.2f%%", value)
            "JPY" -> String.format(Locale.US, "¥%,.2f", value)
            "EUR" -> String.format(Locale.US, "€%,.4f", value)
            else -> if (value >= 1.0) String.format(Locale.US, "$%,.2f", value) else String.format(Locale.US, "$%,.4f", value)
        }
    }

    private fun renderChart(instrument: MarketInstrument) {
        val sparkline = instrument.sparklinePrices
        val candles = instrument.historicalCandles

        if (!sparkline.isNullOrEmpty()) {
            btnToggleIndicator.visibility = View.VISIBLE
            val entries = ArrayList<Entry>()
            val smaEntries = ArrayList<Entry>()
            val window = 5

            sparkline.forEachIndexed { index, price ->
                entries.add(Entry(index.toFloat(), price))
                if (isSmaActive && (index >= window - 1)) {
                    val slice = sparkline.subList(index - window + 1, index + 1)
                    val avg = slice.average().toFloat()
                    smaEntries.add(Entry(index.toFloat(), avg))
                }
            }

            val dataSet = LineDataSet(entries, "Price Trend").apply {
                color = Color.parseColor("#43D9E6")
                setDrawValues(false)
                lineWidth = 2.5f
                setDrawCircles(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = Color.parseColor("#43D9E6")
                fillAlpha = 35
            }

            val dataSets = ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
            dataSets.add(dataSet)

            if (isSmaActive && smaEntries.isNotEmpty()) {
                val smaDataSet = LineDataSet(smaEntries, "SMA 5").apply {
                    color = Color.parseColor("#38BFA1")
                    setDrawValues(false)
                    lineWidth = 1.8f
                    setDrawCircles(false)
                }
                dataSets.add(smaDataSet)
            }

            chartInstrument.data = LineData(dataSets)
            chartInstrument.description.isEnabled = false
            chartInstrument.legend.isEnabled = true
            chartInstrument.legend.textColor = Color.parseColor("#9AA0A6")
            chartInstrument.xAxis.textColor = Color.parseColor("#9AA0A6")
            chartInstrument.axisRight.isEnabled = false
            chartInstrument.axisLeft.textColor = Color.parseColor("#9AA0A6")
            chartInstrument.invalidate()
        } else if (!candles.isNullOrEmpty()) {
            btnToggleIndicator.visibility = View.VISIBLE
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

            val dataSet = LineDataSet(entries, "Price Trend").apply {
                color = Color.parseColor("#43D9E6")
                setDrawValues(false)
                lineWidth = 2.5f
                setDrawCircles(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = Color.parseColor("#43D9E6")
                fillAlpha = 35
            }

            val dataSets = ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
            dataSets.add(dataSet)

            if (isSmaActive && smaEntries.isNotEmpty()) {
                val smaDataSet = LineDataSet(smaEntries, "SMA 5").apply {
                    color = Color.parseColor("#38BFA1")
                    setDrawValues(false)
                    lineWidth = 1.8f
                    setDrawCircles(false)
                }
                dataSets.add(smaDataSet)
            }

            chartInstrument.data = LineData(dataSets)
            chartInstrument.description.isEnabled = false
            chartInstrument.legend.isEnabled = true
            chartInstrument.legend.textColor = Color.parseColor("#9AA0A6")
            chartInstrument.xAxis.textColor = Color.parseColor("#9AA0A6")
            chartInstrument.axisRight.isEnabled = false
            chartInstrument.axisLeft.textColor = Color.parseColor("#9AA0A6")
            chartInstrument.invalidate()
        } else {
            chartInstrument.clear()
            btnToggleIndicator.visibility = View.GONE
        }
    }

    private fun updateWatchlistIcon(isInWatchlist: Boolean) {
        val tintColor = if (isInWatchlist) R.color.accent_teal else R.color.accent_cyan
        btnWatchlistToggle.imageTintList = ContextCompat.getColorStateList(this, tintColor)
    }

    override fun onResume() {
        super.onResume()
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
