package com.repp.aquaticremineztools.presentation.market

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.core.util.DrawerMenuHelper
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.repository.MarketRepository
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import eightbitlab.com.blurview.BlurTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MarketActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var marketBlurTarget: BlurTarget
    private lateinit var viewTopSpacer: View
    private lateinit var btnBack: FrameLayout
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvMarketStatusBanner: TextView

    // Bottom Navigation Bar Items (Homepage Pill Concept)
    private lateinit var tabOverview: FrameLayout
    private lateinit var tabWatchlist: FrameLayout
    private lateinit var tabAlerts: FrameLayout
    private lateinit var tabPortfolio: FrameLayout

    private lateinit var pillOverview: LinearLayout
    private lateinit var pillWatchlist: LinearLayout
    private lateinit var pillAlerts: LinearLayout
    private lateinit var pillPortfolio: LinearLayout

    private lateinit var iconOverview: ImageView
    private lateinit var iconWatchlist: ImageView
    private lateinit var iconAlerts: ImageView
    private lateinit var iconPortfolio: ImageView

    private lateinit var textOverview: TextView
    private lateinit var textWatchlist: TextView
    private lateinit var textAlerts: TextView
    private lateinit var textPortfolio: TextView

    // Destination View Containers
    private lateinit var scrollOverview: View
    private lateinit var layoutWatchlistContainer: View
    private lateinit var layoutAlertsContainer: View
    private lateinit var layoutPortfolioContainer: View

    // Overview Hero Chart
    private lateinit var spinnerHeroBenchmark: Spinner
    private lateinit var tvHeroSymbol: TextView
    private lateinit var tvHeroPrice: TextView
    private lateinit var tvHeroChange: TextView
    private lateinit var tvHeroDataStatus: TextView
    private lateinit var chartHero: LineChart
    private lateinit var btnHero1D: TextView
    private lateinit var btnHero1W: TextView
    private lateinit var btnHero1M: TextView
    private lateinit var btnHero3M: TextView
    private lateinit var btnHero1Y: TextView
    private lateinit var btnHeroALL: TextView

    // Overview Dashboard Section RecyclerViews
    private lateinit var rvMarketPulse: RecyclerView
    private lateinit var rvKeyMarkets: RecyclerView
    private lateinit var rvTopGainers: RecyclerView
    private lateinit var rvTopLosers: RecyclerView
    private lateinit var rvCryptoMarket: RecyclerView
    private lateinit var rvForexMatrix: RecyclerView
    private lateinit var rvStocksAndEtfs: RecyclerView
    private lateinit var rvEconomicIndicators: RecyclerView
    private lateinit var rvEconomicEvents: RecyclerView

    // Watchlist Destination Views
    private lateinit var etWatchlistSearch: EditText
    private lateinit var rvWatchlistGrid: RecyclerView
    private lateinit var layoutWatchlistEmpty: View
    private lateinit var chipWlAll: TextView
    private lateinit var chipWlCrypto: TextView
    private lateinit var chipWlForex: TextView
    private lateinit var chipWlStocks: TextView
    private lateinit var chipWlIndices: TextView
    private lateinit var chipWlCommodities: TextView

    // Alerts Destination Views
    private lateinit var tvActiveAlertsCount: TextView
    private lateinit var tvTriggeredAlertsCount: TextView
    private lateinit var tvPausedAlertsCount: TextView
    private lateinit var btnCreateAlert: Button
    private lateinit var rvAlertsList: RecyclerView
    private lateinit var layoutAlertsEmpty: View

    // Portfolio Destination Views
    private lateinit var tvPortfolioTotalValue: TextView
    private lateinit var tvPortfolioProfitLoss: TextView
    private lateinit var btnAddHolding: Button
    private lateinit var rvHoldingsList: RecyclerView
    private lateinit var layoutPortfolioEmpty: View

    // Adapters
    private lateinit var pulseAdapter: MarketPulseAdapter
    private lateinit var keyMarketsAdapter: KeyMarketsAdapter
    private lateinit var topGainersAdapter: MarketMoversAdapter
    private lateinit var topLosersAdapter: MarketMoversAdapter
    private lateinit var cryptoAdapter: CryptoDashboardAdapter
    private lateinit var forexAdapter: ForexMatrixAdapter
    private lateinit var stockAdapter: StockWatchlistAdapter
    private lateinit var economicIndicatorAdapter: EconomicIndicatorAdapter
    private lateinit var economicEventAdapter: EconomicEventAdapter
    private lateinit var watchlistAdapter: WatchlistAdapter

    private lateinit var viewModel: MarketViewModel
    private var allInstrumentsCache: List<MarketInstrument> = emptyList()
    private var activeWatchlistCategory: MarketCategory? = null
    private var activeWatchlistQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_market)

        bindViews()
        setupWindowInsets()
        setupDrawer()
        setupRecyclerViews()
        setupViewModel()
        setupListeners()
        setupHeroChartDefaults()
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        marketBlurTarget = findViewById(R.id.marketBlurTarget)
        viewTopSpacer = findViewById(R.id.viewTopSpacer)
        btnBack = findViewById(R.id.btnBack)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvMarketStatusBanner = findViewById(R.id.tvMarketStatusBanner)

        // Bottom Navigation Tabs
        tabOverview = findViewById(R.id.tabOverview)
        tabWatchlist = findViewById(R.id.tabWatchlist)
        tabAlerts = findViewById(R.id.tabAlerts)
        tabPortfolio = findViewById(R.id.tabPortfolio)

        pillOverview = findViewById(R.id.pillOverview)
        pillWatchlist = findViewById(R.id.pillWatchlist)
        pillAlerts = findViewById(R.id.pillAlerts)
        pillPortfolio = findViewById(R.id.pillPortfolio)

        iconOverview = findViewById(R.id.iconOverview)
        iconWatchlist = findViewById(R.id.iconWatchlist)
        iconAlerts = findViewById(R.id.iconAlerts)
        iconPortfolio = findViewById(R.id.iconPortfolio)

        textOverview = findViewById(R.id.textOverview)
        textWatchlist = findViewById(R.id.textWatchlist)
        textAlerts = findViewById(R.id.textAlerts)
        textPortfolio = findViewById(R.id.textPortfolio)

        // Destination Containers
        scrollOverview = findViewById(R.id.scrollOverview)
        layoutWatchlistContainer = findViewById(R.id.layoutWatchlistContainer)
        layoutAlertsContainer = findViewById(R.id.layoutAlertsContainer)
        layoutPortfolioContainer = findViewById(R.id.layoutPortfolioContainer)

        // Hero Chart
        spinnerHeroBenchmark = findViewById(R.id.spinnerHeroBenchmark)
        tvHeroSymbol = findViewById(R.id.tvHeroSymbol)
        tvHeroPrice = findViewById(R.id.tvHeroPrice)
        tvHeroChange = findViewById(R.id.tvHeroChange)
        tvHeroDataStatus = findViewById(R.id.tvHeroDataStatus)
        chartHero = findViewById(R.id.chartHero)
        btnHero1D = findViewById(R.id.btnHero1D)
        btnHero1W = findViewById(R.id.btnHero1W)
        btnHero1M = findViewById(R.id.btnHero1M)
        btnHero3M = findViewById(R.id.btnHero3M)
        btnHero1Y = findViewById(R.id.btnHero1Y)
        btnHeroALL = findViewById(R.id.btnHeroALL)

        // Dashboard Section RecyclerViews
        rvMarketPulse = findViewById(R.id.rvMarketPulse)
        rvKeyMarkets = findViewById(R.id.rvKeyMarkets)
        rvTopGainers = findViewById(R.id.rvTopGainers)
        rvTopLosers = findViewById(R.id.rvTopLosers)
        rvCryptoMarket = findViewById(R.id.rvCryptoMarket)
        rvForexMatrix = findViewById(R.id.rvForexMatrix)
        rvStocksAndEtfs = findViewById(R.id.rvStocksAndEtfs)
        rvEconomicIndicators = findViewById(R.id.rvEconomicIndicators)
        rvEconomicEvents = findViewById(R.id.rvEconomicEvents)

        // Watchlist Views
        etWatchlistSearch = findViewById(R.id.etWatchlistSearch)
        rvWatchlistGrid = findViewById(R.id.rvWatchlistGrid)
        layoutWatchlistEmpty = findViewById(R.id.layoutWatchlistEmpty)
        chipWlAll = findViewById(R.id.chipWlAll)
        chipWlCrypto = findViewById(R.id.chipWlCrypto)
        chipWlForex = findViewById(R.id.chipWlForex)
        chipWlStocks = findViewById(R.id.chipWlStocks)
        chipWlIndices = findViewById(R.id.chipWlIndices)
        chipWlCommodities = findViewById(R.id.chipWlCommodities)

        // Alerts Views
        tvActiveAlertsCount = findViewById(R.id.tvActiveAlertsCount)
        tvTriggeredAlertsCount = findViewById(R.id.tvTriggeredAlertsCount)
        tvPausedAlertsCount = findViewById(R.id.tvPausedAlertsCount)
        btnCreateAlert = findViewById(R.id.btnCreateAlert)
        rvAlertsList = findViewById(R.id.rvAlertsList)
        layoutAlertsEmpty = findViewById(R.id.layoutAlertsEmpty)

        // Portfolio Views
        tvPortfolioTotalValue = findViewById(R.id.tvPortfolioTotalValue)
        tvPortfolioProfitLoss = findViewById(R.id.tvPortfolioProfitLoss)
        btnAddHolding = findViewById(R.id.btnAddHolding)
        rvHoldingsList = findViewById(R.id.rvHoldingsList)
        layoutPortfolioEmpty = findViewById(R.id.layoutPortfolioEmpty)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContentContainer)) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val lpSpacer = viewTopSpacer.layoutParams
            lpSpacer.height = statusBarHeight
            viewTopSpacer.layoutParams = lpSpacer
            insets
        }
    }

    private fun setupDrawer() {
        DrawerMenuHelper.setup(this, drawerLayout, emptyList())
    }

    private fun setupRecyclerViews() {
        // Section 1: Market Pulse (Horizontal)
        pulseAdapter = MarketPulseAdapter { showInstrumentBottomSheet(it) }
        rvMarketPulse.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMarketPulse.adapter = pulseAdapter

        // Section 3: Key Markets (Horizontal)
        keyMarketsAdapter = KeyMarketsAdapter { showInstrumentBottomSheet(it) }
        rvKeyMarkets.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvKeyMarkets.adapter = keyMarketsAdapter

        // Section 4: Top Gainers & Losers (Vertical)
        topGainersAdapter = MarketMoversAdapter { showInstrumentBottomSheet(it) }
        rvTopGainers.layoutManager = LinearLayoutManager(this)
        rvTopGainers.adapter = topGainersAdapter

        topLosersAdapter = MarketMoversAdapter { showInstrumentBottomSheet(it) }
        rvTopLosers.layoutManager = LinearLayoutManager(this)
        rvTopLosers.adapter = topLosersAdapter

        // Section 5: Crypto Market (Vertical)
        cryptoAdapter = CryptoDashboardAdapter { showInstrumentBottomSheet(it) }
        rvCryptoMarket.layoutManager = LinearLayoutManager(this)
        rvCryptoMarket.adapter = cryptoAdapter

        // Section 6: Forex Matrix (Horizontal)
        forexAdapter = ForexMatrixAdapter { showInstrumentBottomSheet(it) }
        rvForexMatrix.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvForexMatrix.adapter = forexAdapter

        // Section 7: Stocks & ETFs (Vertical)
        stockAdapter = StockWatchlistAdapter { showInstrumentBottomSheet(it) }
        rvStocksAndEtfs.layoutManager = LinearLayoutManager(this)
        rvStocksAndEtfs.adapter = stockAdapter

        // Section 8: Economic Indicators (Horizontal)
        economicIndicatorAdapter = EconomicIndicatorAdapter { showInstrumentBottomSheet(it) }
        rvEconomicIndicators.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvEconomicIndicators.adapter = economicIndicatorAdapter

        // Section 8 (Events): Economic Events (Vertical)
        economicEventAdapter = EconomicEventAdapter()
        rvEconomicEvents.layoutManager = LinearLayoutManager(this)
        rvEconomicEvents.adapter = economicEventAdapter

        // Watchlist Grid
        watchlistAdapter = WatchlistAdapter { showInstrumentBottomSheet(it) }
        val isWideScreen = resources.configuration.screenWidthDp >= 600
        rvWatchlistGrid.layoutManager = GridLayoutManager(this, if (isWideScreen) 2 else 1)
        rvWatchlistGrid.adapter = watchlistAdapter
    }

    private fun setupHeroChartDefaults() {
        chartHero.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.isEnabled = false
            axisRight.isEnabled = false

            xAxis.apply {
                textColor = Color.parseColor("#9AA0A6")
                setDrawGridLines(false)
                setDrawAxisLine(false)
            }

            axisLeft.apply {
                textColor = Color.parseColor("#9AA0A6")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1B1F25")
                setDrawAxisLine(false)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MarketViewModel::class.java]

        viewModel.currentDestination.observe(this) { destination ->
            switchDestination(destination)
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MarketUiState.Loading -> {
                    // Retain current view
                }
                is MarketUiState.Success -> {
                    allInstrumentsCache = state.allInstruments
                    pulseAdapter.updateData(state.marketPulse)
                    keyMarketsAdapter.updateData(state.keyMarkets)
                    topGainersAdapter.updateData(state.topGainers)
                    topLosersAdapter.updateData(state.topLosers)
                    cryptoAdapter.updateData(state.cryptoInstruments)
                    forexAdapter.updateData(state.forexInstruments)
                    stockAdapter.updateData(state.stocksInstruments)
                    economicIndicatorAdapter.updateData(state.economicIndicators)
                    economicEventAdapter.updateData(state.economicEvents)

                    if (!state.bannerMessage.isNullOrEmpty()) {
                        tvMarketStatusBanner.text = state.bannerMessage
                        tvMarketStatusBanner.visibility = View.VISIBLE
                    } else {
                        tvMarketStatusBanner.visibility = View.GONE
                    }

                    // Update Hero Benchmark Instrument
                    val heroInst = state.allInstruments.find { it.id == state.selectedHeroBenchmarkId }
                        ?: state.allInstruments.firstOrNull()
                    heroInst?.let { renderHeroChart(it) }

                    // Refresh active Watchlist/Alerts/Portfolio view data
                    refreshWatchlistData()
                    refreshAlertsData()
                    refreshPortfolioData()
                }
                is MarketUiState.Error -> {
                    tvMarketStatusBanner.text = state.message
                    tvMarketStatusBanner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupListeners() {
        // BACK BUTTON ALWAYS RETURNS TO HOMEPAGE (HomeActivity)
        btnBack.setOnClickListener {
            finish()
        }

        // Homepage-style Bottom Navigation Bar Tabs
        tabOverview.setOnClickListener { viewModel.setDestination(MarketNavDestination.OVERVIEW) }
        tabWatchlist.setOnClickListener { viewModel.setDestination(MarketNavDestination.WATCHLIST) }
        tabAlerts.setOnClickListener { viewModel.setDestination(MarketNavDestination.ALERTS) }
        tabPortfolio.setOnClickListener { viewModel.setDestination(MarketNavDestination.PORTFOLIO) }

        // Hero Time Range Buttons
        val rangeButtons = listOf(
            btnHero1D to "1D", btnHero1W to "1W", btnHero1M to "1M",
            btnHero3M to "3M", btnHero1Y to "1Y", btnHeroALL to "ALL"
        )
        rangeButtons.forEach { (btn, label) ->
            btn.setOnClickListener {
                rangeButtons.forEach { (b, _) ->
                    b.setBackgroundResource(R.drawable.bg_chip_inactive)
                    b.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
                btn.setBackgroundResource(R.drawable.bg_chip_cyan_active)
                btn.setTextColor(ContextCompat.getColor(this, R.color.accent_cyan))
                viewModel.setHeroTimeRange(label)
            }
        }

        // Hero Benchmark Spinner
        setupBenchmarkSpinner()

        // Watchlist Search & Category Filter
        etWatchlistSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                activeWatchlistQuery = s?.toString()?.trim() ?: ""
                refreshWatchlistData()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipWlAll.setOnClickListener { selectWatchlistCategory(null, chipWlAll) }
        chipWlCrypto.setOnClickListener { selectWatchlistCategory(MarketCategory.CRYPTO, chipWlCrypto) }
        chipWlForex.setOnClickListener { selectWatchlistCategory(MarketCategory.FOREX, chipWlForex) }
        chipWlStocks.setOnClickListener { selectWatchlistCategory(MarketCategory.STOCKS, chipWlStocks) }
        chipWlIndices.setOnClickListener { selectWatchlistCategory(MarketCategory.INDICES, chipWlIndices) }
        chipWlCommodities.setOnClickListener { selectWatchlistCategory(MarketCategory.COMMODITIES, chipWlCommodities) }

        // Alerts & Portfolio Action Buttons
        btnCreateAlert.setOnClickListener {
            val sheet = MarketAlertBottomSheet.newInstance()
            sheet.onAlertCreated = { refreshAlertsData() }
            sheet.show(supportFragmentManager, "MarketAlertBottomSheet")
        }

        btnAddHolding.setOnClickListener {
            val sheet = PortfolioHoldingBottomSheet.newInstance()
            sheet.onHoldingAdded = { refreshPortfolioData() }
            sheet.show(supportFragmentManager, "PortfolioHoldingBottomSheet")
        }
    }

    private fun setupBenchmarkSpinner() {
        val benchmarks = listOf(
            "SPX" to "sp500",
            "IHSG" to "ihsg",
            "IXIC" to "nasdaq",
            "BTC" to "bitcoin",
            "USD/IDR" to "usd_idr"
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            benchmarks.map { it.first }
        )
        spinnerHeroBenchmark.adapter = adapter
        spinnerHeroBenchmark.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedId = benchmarks.getOrNull(position)?.second ?: "sp500"
                viewModel.setHeroBenchmark(selectedId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun switchDestination(destination: MarketNavDestination) {
        val activeTextColor = ContextCompat.getColor(this, R.color.text_primary)
        val inactiveIconColor = ContextCompat.getColor(this, R.color.text_secondary)

        // Reset all 4 tabs to inactive
        val pills = listOf(pillOverview, pillWatchlist, pillAlerts, pillPortfolio)
        val icons = listOf(iconOverview, iconWatchlist, iconAlerts, iconPortfolio)
        val texts = listOf(textOverview, textWatchlist, textAlerts, textPortfolio)

        pills.forEach { it.setBackgroundColor(Color.TRANSPARENT) }
        icons.forEach { it.setColorFilter(inactiveIconColor) }
        texts.forEach { it.visibility = View.GONE }

        // Hide all destination containers
        scrollOverview.visibility = View.GONE
        layoutWatchlistContainer.visibility = View.GONE
        layoutAlertsContainer.visibility = View.GONE
        layoutPortfolioContainer.visibility = View.GONE

        when (destination) {
            MarketNavDestination.OVERVIEW -> {
                tvHeaderTitle.text = "OVERVIEW"
                pillOverview.setBackgroundResource(R.drawable.bg_nav_active_pill)
                iconOverview.setColorFilter(activeTextColor)
                textOverview.visibility = View.VISIBLE
                scrollOverview.visibility = View.VISIBLE
            }
            MarketNavDestination.WATCHLIST -> {
                tvHeaderTitle.text = "WATCHLIST"
                pillWatchlist.setBackgroundResource(R.drawable.bg_nav_active_pill)
                iconWatchlist.setColorFilter(activeTextColor)
                textWatchlist.visibility = View.VISIBLE
                layoutWatchlistContainer.visibility = View.VISIBLE
                refreshWatchlistData()
            }
            MarketNavDestination.ALERTS -> {
                tvHeaderTitle.text = "ALERTS"
                pillAlerts.setBackgroundResource(R.drawable.bg_nav_active_pill)
                iconAlerts.setColorFilter(activeTextColor)
                textAlerts.visibility = View.VISIBLE
                layoutAlertsContainer.visibility = View.VISIBLE
                refreshAlertsData()
            }
            MarketNavDestination.PORTFOLIO -> {
                tvHeaderTitle.text = "PORTFOLIO"
                pillPortfolio.setBackgroundResource(R.drawable.bg_nav_active_pill)
                iconPortfolio.setColorFilter(activeTextColor)
                textPortfolio.visibility = View.VISIBLE
                layoutPortfolioContainer.visibility = View.VISIBLE
                refreshPortfolioData()
            }
        }
    }

    private fun renderHeroChart(instrument: MarketInstrument) {
        tvHeroSymbol.text = "${instrument.symbol} · ${instrument.name}"

        val price = instrument.currentPrice
        if (price != null) {
            val formatted = when (instrument.currency) {
                "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                "%" -> String.format(Locale.US, "%.2f%%", price)
                "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                "EUR" -> String.format(Locale.US, "€%,.4f", price)
                else -> if (price >= 1.0) String.format(Locale.US, "$%,.2f", price) else String.format(Locale.US, "$%,.4f", price)
            }
            tvHeroPrice.text = formatted
        } else {
            tvHeroPrice.text = "--"
        }

        val pctChange = instrument.priceChangePercentage24h
        if (pctChange != null) {
            val isPos = pctChange >= 0
            val sign = if (isPos) "+" else ""
            tvHeroChange.text = String.format(Locale.US, "%s%.2f%% (24h)", sign, pctChange)
            val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
            tvHeroChange.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            tvHeroChange.text = "--"
        }

        tvHeroDataStatus.text = when (instrument.dataStatus) {
            MarketDataStatus.LIVE -> "LIVE FEED"
            MarketDataStatus.CACHED -> "CACHED"
            MarketDataStatus.SIMULATED -> "SIMULATED"
            MarketDataStatus.UNAVAILABLE -> "UNAVAILABLE"
        }

        val sparkline = instrument.sparklinePrices
        val entries = ArrayList<Entry>()
        if (!sparkline.isNullOrEmpty()) {
            sparkline.forEachIndexed { i, p -> entries.add(Entry(i.toFloat(), p)) }
        } else {
            val basePrice = (price ?: 100.0).toFloat()
            val dummyPoints = listOf(
                basePrice * 0.98f, basePrice * 0.985f, basePrice * 0.992f,
                basePrice * 0.99f, basePrice * 1.005f, basePrice * 1.008f, basePrice
            )
            dummyPoints.forEachIndexed { i, p -> entries.add(Entry(i.toFloat(), p)) }
        }

        val dataSet = LineDataSet(entries, "Price Trend").apply {
            color = Color.parseColor("#43D9E6")
            lineWidth = 2.5f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#43D9E6")
            fillAlpha = 35
        }

        chartHero.data = LineData(dataSet)
        chartHero.animateX(400)
        chartHero.invalidate()
    }

    private fun selectWatchlistCategory(category: MarketCategory?, selectedChip: TextView) {
        activeWatchlistCategory = category
        val chips = listOf(chipWlAll, chipWlCrypto, chipWlForex, chipWlStocks, chipWlIndices, chipWlCommodities)
        chips.forEach { chip ->
            chip.setBackgroundResource(R.drawable.bg_chip_inactive)
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
        selectedChip.setBackgroundResource(R.drawable.bg_chip_cyan_active)
        selectedChip.setTextColor(ContextCompat.getColor(this, R.color.accent_cyan))
        refreshWatchlistData()
    }

    private fun refreshWatchlistData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@MarketActivity)
            val watchlistEntries = repository.getWatchlist()
            val watchlistIds = watchlistEntries.map { it.instrumentId }.toSet()

            var displayedList = if (allInstrumentsCache.isNotEmpty()) {
                allInstrumentsCache.filter { watchlistIds.contains(it.id) || watchlistEntries.any { w -> w.symbol.equals(it.symbol, true) } }
            } else {
                watchlistEntries.map { entry ->
                    MarketInstrument(
                        id = entry.instrumentId,
                        symbol = entry.symbol,
                        name = entry.name,
                        category = entry.category,
                        currentPrice = null,
                        priceChange24h = null,
                        priceChangePercentage24h = null,
                        dataStatus = MarketDataStatus.UNAVAILABLE
                    )
                }
            }

            if (activeWatchlistCategory != null) {
                displayedList = displayedList.filter { it.category == activeWatchlistCategory }
            }

            if (activeWatchlistQuery.isNotEmpty()) {
                displayedList = displayedList.filter {
                    it.symbol.contains(activeWatchlistQuery, ignoreCase = true) ||
                            it.name.contains(activeWatchlistQuery, ignoreCase = true)
                }
            }

            withContext(Dispatchers.Main) {
                if (displayedList.isEmpty()) {
                    rvWatchlistGrid.visibility = View.GONE
                    layoutWatchlistEmpty.visibility = View.VISIBLE
                } else {
                    rvWatchlistGrid.visibility = View.VISIBLE
                    layoutWatchlistEmpty.visibility = View.GONE
                    watchlistAdapter.updateData(displayedList)
                }
            }
        }
    }

    private fun refreshAlertsData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@MarketActivity)
            val alerts = repository.getAlerts()

            val activeCount = alerts.count { it.isActive && !it.isTriggered }
            val triggeredCount = alerts.count { it.isTriggered }
            val pausedCount = alerts.count { !it.isActive }

            withContext(Dispatchers.Main) {
                tvActiveAlertsCount.text = activeCount.toString()
                tvTriggeredAlertsCount.text = triggeredCount.toString()
                tvPausedAlertsCount.text = pausedCount.toString()

                if (alerts.isEmpty()) {
                    rvAlertsList.visibility = View.GONE
                    layoutAlertsEmpty.visibility = View.VISIBLE
                } else {
                    rvAlertsList.visibility = View.VISIBLE
                    layoutAlertsEmpty.visibility = View.GONE
                    rvAlertsList.layoutManager = LinearLayoutManager(this@MarketActivity)
                    rvAlertsList.adapter = AlertsActivity.AlertsAdapter(alerts) { alertToDelete ->
                        repository.deleteAlert(alertToDelete.id)
                        refreshAlertsData()
                    }
                }
            }
        }
    }

    private fun refreshPortfolioData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@MarketActivity)
            val holdings = repository.getPortfolioHoldings()
            val instrumentsResult = repository.getAllMarketInstruments(forceRefresh = false)
            val instrumentsMap = instrumentsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()

            val valuation = repository.calculatePortfolioValuation(holdings, instrumentsMap)

            val holdingsDisplayList = holdings.map { holding ->
                val inst = instrumentsMap[holding.instrumentId]
                val currentPrice = inst?.currentPrice ?: holding.averageCost
                val value = holding.quantity * currentPrice
                val cost = holding.quantity * holding.averageCost

                MarketInstrument(
                    id = holding.id,
                    symbol = holding.symbol,
                    name = "${holding.name} (${holding.quantity} units)",
                    category = inst?.category ?: MarketCategory.OVERVIEW,
                    currentPrice = value,
                    priceChange24h = value - cost,
                    priceChangePercentage24h = if (cost > 0.0) ((value - cost) / cost) * 100.0 else 0.0,
                    currency = holding.currency,
                    exchange = "HOLDING",
                    imageUrl = inst?.imageUrl
                )
            }

            withContext(Dispatchers.Main) {
                val totalValStr = if (valuation.totalValuationIdr > 0) {
                    String.format(
                        Locale.US,
                        "$%,.2f USD  |  Rp %,.2f IDR",
                        valuation.totalValuationUsd,
                        valuation.totalValuationIdr
                    )
                } else {
                    String.format(Locale.US, "$%,.2f USD", valuation.totalValuationUsd)
                }
                tvPortfolioTotalValue.text = totalValStr

                val plUsdSign = if (valuation.unrealizedPlUsd >= 0) "+" else ""
                val plText = if (valuation.hasUnpricedHoldings) {
                    String.format(
                        Locale.US,
                        "Unrealized P/L: %s$%,.2f (%.2f%%) · Contains Fallback Prices",
                        plUsdSign,
                        valuation.unrealizedPlUsd,
                        valuation.unrealizedPlPctUsd
                    )
                } else {
                    String.format(
                        Locale.US,
                        "Unrealized P/L: %s$%,.2f (%.2f%%)",
                        plUsdSign,
                        valuation.unrealizedPlUsd,
                        valuation.unrealizedPlPctUsd
                    )
                }
                tvPortfolioProfitLoss.text = plText

                if (holdings.isEmpty()) {
                    rvHoldingsList.visibility = View.GONE
                    layoutPortfolioEmpty.visibility = View.VISIBLE
                } else {
                    rvHoldingsList.visibility = View.VISIBLE
                    layoutPortfolioEmpty.visibility = View.GONE
                    val adapter = MarketInstrumentAdapter(holdingsDisplayList) { displayInst ->
                        val foundHolding = holdings.find { it.id == displayInst.id }
                        if (foundHolding != null) {
                            showDeleteHoldingDialog(foundHolding)
                        }
                    }
                    rvHoldingsList.layoutManager = LinearLayoutManager(this@MarketActivity)
                    rvHoldingsList.adapter = adapter
                }
            }
        }
    }

    private fun showDeleteHoldingDialog(holding: PortfolioHolding) {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Remove Holding")
            .setMessage("Do you want to remove ${holding.symbol} (${holding.quantity} units) from your portfolio?")
            .setPositiveButton("Remove") { _, _ ->
                val repository = MarketRepository.getInstance(this)
                repository.removePortfolioHolding(holding.id)
                refreshPortfolioData()
                Toast.makeText(this, "Holding removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showInstrumentBottomSheet(instrument: MarketInstrument) {
        val bottomSheet = MarketInstrumentBottomSheet.newInstance(instrument)
        bottomSheet.show(supportFragmentManager, "MarketInstrumentBottomSheet")
    }

    override fun onResume() {
        super.onResume()
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        } else {
            viewModel.loadMarket(this, forceRefresh = false)
        }
    }
}
