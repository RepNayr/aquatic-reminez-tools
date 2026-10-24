package com.repp.aquaticremineztools.presentation.market

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
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
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class MarketActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var marketBlurTarget: BlurTarget
    private lateinit var blurSearchBar: BlurView
    private lateinit var etMarketSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var btnMenu: FrameLayout
    private lateinit var btnDemoToggle: TextView
    private lateinit var btnAlerts: ImageView
    private lateinit var btnPortfolio: ImageView
    private lateinit var layoutCategoryChips: LinearLayout
    private lateinit var tvMarketStatusBanner: TextView
    private lateinit var rvMarketInstruments: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyStateTitle: TextView
    private lateinit var tvEmptyStateSubtitle: TextView

    private lateinit var viewModel: MarketViewModel
    private lateinit var adapter: MarketInstrumentAdapter

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
        setupGlassBlur()
        setupDrawer()
        setupCategoryChips()
        setupRecyclerView()
        setupViewModel()
        setupListeners()
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        marketBlurTarget = findViewById(R.id.marketBlurTarget)
        blurSearchBar = findViewById(R.id.blurSearchBar)
        etMarketSearch = findViewById(R.id.etMarketSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnMenu = findViewById(R.id.btnMenu)
        btnDemoToggle = findViewById(R.id.btnDemoToggle)
        btnAlerts = findViewById(R.id.btnAlerts)
        btnPortfolio = findViewById(R.id.btnPortfolio)
        layoutCategoryChips = findViewById(R.id.layoutCategoryChips)
        tvMarketStatusBanner = findViewById(R.id.tvMarketStatusBanner)
        rvMarketInstruments = findViewById(R.id.rvMarketInstruments)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyStateTitle = findViewById(R.id.tvEmptyStateTitle)
        tvEmptyStateSubtitle = findViewById(R.id.tvEmptyStateSubtitle)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContentContainer)) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            val layoutToolbar = findViewById<LinearLayout>(R.id.layoutToolbar)
            layoutToolbar.setPadding(
                layoutToolbar.paddingLeft,
                statusBarHeight,
                layoutToolbar.paddingRight,
                layoutToolbar.paddingBottom
            )

            insets
        }
    }

    private fun setupGlassBlur() {
        val clearDrawable = window.decorView.background
        GlassBlurHelper.setupBlur(blurSearchBar, marketBlurTarget, clearDrawable)
    }

    private fun setupDrawer() {
        DrawerMenuHelper.setup(this, drawerLayout, listOf(blurSearchBar))
    }

    private fun setupCategoryChips() {
        layoutCategoryChips.removeAllViews()
        val categories = MarketCategory.entries
        val inflater = layoutInflater

        categories.forEach { category ->
            val chipView = inflater.inflate(R.layout.item_news_category_chip, layoutCategoryChips, false)
            val tvChipName = chipView.findViewById<TextView>(R.id.tvCategoryName)
            tvChipName.text = category.displayName

            chipView.setOnClickListener {
                when (category) {
                    MarketCategory.PORTFOLIO -> {
                        openPortfolioActivity()
                    }
                    MarketCategory.ALERTS -> {
                        openAlertsActivity()
                    }
                    else -> {
                        viewModel.setCategory(this, category)
                        highlightCategoryChip(category)
                    }
                }
            }
            layoutCategoryChips.addView(chipView)
        }
    }

    private fun highlightCategoryChip(selectedCategory: MarketCategory) {
        for (i in 0 until layoutCategoryChips.childCount) {
            val child = layoutCategoryChips.getChildAt(i)
            val tv = child.findViewById<TextView>(R.id.tvCategoryName)
            val isSelected = MarketCategory.entries[i] == selectedCategory
            if (isSelected) {
                tv.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))
                child.background = ContextCompat.getDrawable(this, R.drawable.bg_nav_active_pill)
            } else {
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                child.background = null
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MarketInstrumentAdapter { instrument ->
            val intent = Intent(this, InstrumentDetailActivity::class.java).apply {
                putExtra("instrument_id", instrument.id)
                putExtra("instrument_symbol", instrument.symbol)
                putExtra("instrument_name", instrument.name)
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }
        rvMarketInstruments.layoutManager = LinearLayoutManager(this)
        rvMarketInstruments.adapter = adapter
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
                    adapter.updateData(state.filteredInstruments)
                    highlightCategoryChip(state.selectedCategory)

                    btnDemoToggle.text = if (state.isDemoMode) "DEMO: ON" else "DEMO: OFF"
                    val demoTextColor = if (state.isDemoMode) R.color.accent_teal else R.color.text_muted
                    btnDemoToggle.setTextColor(ContextCompat.getColor(this, demoTextColor))

                    if (state.filteredInstruments.isEmpty()) {
                        rvMarketInstruments.visibility = View.GONE
                        layoutEmptyState.visibility = View.VISIBLE
                        tvEmptyStateTitle.text = state.emptyStateTitle
                        tvEmptyStateSubtitle.text = state.emptyStateSubtitle
                    } else {
                        rvMarketInstruments.visibility = View.VISIBLE
                        layoutEmptyState.visibility = View.GONE
                    }

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
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnDemoToggle.setOnClickListener {
            viewModel.toggleDemoMode(this)
        }

        btnAlerts.setOnClickListener {
            openAlertsActivity()
        }

        btnPortfolio.setOnClickListener {
            openPortfolioActivity()
        }

        btnClearSearch.setOnClickListener {
            etMarketSearch.setText("")
        }

        etMarketSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.setSearchQuery(this@MarketActivity, query)
            }
        })
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
