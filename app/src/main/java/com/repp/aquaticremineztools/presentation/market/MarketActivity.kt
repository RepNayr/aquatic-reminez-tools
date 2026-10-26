package com.repp.aquaticremineztools.presentation.market

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.core.util.DrawerMenuHelper
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import eightbitlab.com.blurview.BlurTarget

class MarketActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var marketBlurTarget: BlurTarget
    private lateinit var viewTopSpacer: View
    private lateinit var btnBack: FrameLayout
    private lateinit var btnAlerts: ImageView
    private lateinit var btnPortfolio: ImageView
    private lateinit var tvMarketStatusBanner: TextView

    // Dashboard Section RecyclerViews
    private lateinit var rvMarketPulse: RecyclerView
    private lateinit var rvKeyMarkets: RecyclerView
    private lateinit var rvCryptoMarket: RecyclerView
    private lateinit var rvForexMatrix: RecyclerView
    private lateinit var rvStocksAndEtfs: RecyclerView
    private lateinit var rvCommodities: RecyclerView
    private lateinit var rvEconomicIndicators: RecyclerView
    private lateinit var rvEconomicEvents: RecyclerView

    // Adapters
    private lateinit var pulseAdapter: MarketPulseAdapter
    private lateinit var keyMarketsAdapter: KeyMarketsAdapter
    private lateinit var cryptoAdapter: CryptoDashboardAdapter
    private lateinit var forexAdapter: ForexMatrixAdapter
    private lateinit var stockAdapter: StockWatchlistAdapter
    private lateinit var commodityAdapter: CommodityAdapter
    private lateinit var economicIndicatorAdapter: EconomicIndicatorAdapter
    private lateinit var economicEventAdapter: EconomicEventAdapter

    private lateinit var viewModel: MarketViewModel

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
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        marketBlurTarget = findViewById(R.id.marketBlurTarget)
        viewTopSpacer = findViewById(R.id.viewTopSpacer)
        btnBack = findViewById(R.id.btnBack)
        btnAlerts = findViewById(R.id.btnAlerts)
        btnPortfolio = findViewById(R.id.btnPortfolio)
        tvMarketStatusBanner = findViewById(R.id.tvMarketStatusBanner)

        rvMarketPulse = findViewById(R.id.rvMarketPulse)
        rvKeyMarkets = findViewById(R.id.rvKeyMarkets)
        rvCryptoMarket = findViewById(R.id.rvCryptoMarket)
        rvForexMatrix = findViewById(R.id.rvForexMatrix)
        rvStocksAndEtfs = findViewById(R.id.rvStocksAndEtfs)
        rvCommodities = findViewById(R.id.rvCommodities)
        rvEconomicIndicators = findViewById(R.id.rvEconomicIndicators)
        rvEconomicEvents = findViewById(R.id.rvEconomicEvents)
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
        // Section A: Market Pulse (Horizontal)
        pulseAdapter = MarketPulseAdapter { showInstrumentBottomSheet(it) }
        rvMarketPulse.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMarketPulse.adapter = pulseAdapter

        // Section B: Key Markets (Horizontal)
        keyMarketsAdapter = KeyMarketsAdapter { showInstrumentBottomSheet(it) }
        rvKeyMarkets.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvKeyMarkets.adapter = keyMarketsAdapter

        // Section C: Crypto Market (Vertical)
        cryptoAdapter = CryptoDashboardAdapter { showInstrumentBottomSheet(it) }
        rvCryptoMarket.layoutManager = LinearLayoutManager(this)
        rvCryptoMarket.adapter = cryptoAdapter

        // Section D: Forex Matrix (Horizontal)
        forexAdapter = ForexMatrixAdapter { showInstrumentBottomSheet(it) }
        rvForexMatrix.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvForexMatrix.adapter = forexAdapter

        // Section E: Stocks & ETFs (Vertical)
        stockAdapter = StockWatchlistAdapter { showInstrumentBottomSheet(it) }
        rvStocksAndEtfs.layoutManager = LinearLayoutManager(this)
        rvStocksAndEtfs.adapter = stockAdapter

        // Section F: Commodities (Horizontal)
        commodityAdapter = CommodityAdapter { showInstrumentBottomSheet(it) }
        rvCommodities.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCommodities.adapter = commodityAdapter

        // Section G: Economic Indicators (Horizontal)
        economicIndicatorAdapter = EconomicIndicatorAdapter { showInstrumentBottomSheet(it) }
        rvEconomicIndicators.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvEconomicIndicators.adapter = economicIndicatorAdapter

        // Section G2: Economic Events (Vertical)
        economicEventAdapter = EconomicEventAdapter()
        rvEconomicEvengit s.layoutManager = LinearLayoutManager(this)
        rvEconomicEvents.adapter = economicEventAdapter
    }

    @SuppressLint("SetTextI18n")
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MarketViewModel::class.java]

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MarketUiState.Loading -> {
                    // Retain current view
                }
                is MarketUiState.Success -> {
                    pulseAdapter.updateData(state.marketPulse)
                    keyMarketsAdapter.updateData(state.keyMarkets)
                    cryptoAdapter.updateData(state.cryptoInstruments)
                    forexAdapter.updateData(state.forexInstruments)
                    stockAdapter.updateData(state.stocksInstruments)
                    commodityAdapter.updateData(state.commoditiesInstruments)
                    economicIndicatorAdapter.updateData(state.economicIndicators)
                    economicEventAdapter.updateData(state.economicEvents)

                    if (!state.bannerMessage.isNullOrEmpty()) {
                        tvMarketStatusBanner.text = state.bannerMessage
                        tvMarketStatusBanner.visibility = View.VISIBLE
                    } else {
                        tvMarketStatusBanner.visibility = View.GONE
                    }
                }
                is MarketUiState.Error -> {
                    tvMarketStatusBanner.text = state.message
                    tvMarketStatusBanner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAlerts.setOnClickListener {
            openAlertsActivity()
        }

        btnPortfolio.setOnClickListener {
            openPortfolioActivity()
        }
    }

    private fun showInstrumentBottomSheet(instrument: MarketInstrument) {
        val bottomSheet = MarketInstrumentBottomSheet.newInstance(instrument)
        bottomSheet.show(supportFragmentManager, "MarketInstrumentBottomSheet")
    }

    private fun openPortfolioActivity() {
        val intent = Intent(this, PortfolioActivity::class.java)
        ActivityTransition.startActivitySmooth(this, intent)
    }

    private fun openAlertsActivity() {
        val intent = Intent(this, AlertsActivity::class.java)
        ActivityTransition.startActivitySmooth(this, intent)
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
