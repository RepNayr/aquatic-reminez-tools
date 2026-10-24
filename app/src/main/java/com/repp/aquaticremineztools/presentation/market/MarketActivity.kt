package com.repp.aquaticremineztools.presentation.market

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
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
import com.repp.aquaticremineztools.presentation.about.AboutActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import com.repp.aquaticremineztools.presentation.settings.SettingsActivity
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class MarketActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var marketBlurTarget: BlurTarget
    private lateinit var blurSearchBar: BlurView
    private lateinit var etMarketSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var btnMenu: FrameLayout
    private lateinit var btnPortfolio: ImageView
    private lateinit var layoutCategoryChips: LinearLayout
    private lateinit var tvMarketStatusBanner: TextView
    private lateinit var rvMarketInstruments: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyStateTitle: TextView
    private lateinit var tvEmptyStateSubtitle: TextView

    private lateinit var viewModel: MarketViewModel
    private lateinit var adapter: MarketInstrumentAdapter

    // Bottom Navigation
    private lateinit var tabHome: FrameLayout
    private lateinit var tabSearch: FrameLayout
    private lateinit var tabHistory: FrameLayout
    private lateinit var tabAbout: FrameLayout
    private lateinit var tabSettings: FrameLayout

    private lateinit var pillHome: LinearLayout
    private lateinit var pillSearch: LinearLayout
    private lateinit var pillHistory: LinearLayout
    private lateinit var pillAbout: LinearLayout
    private lateinit var pillSettings: LinearLayout

    private lateinit var iconHome: ImageView
    private lateinit var iconSearch: ImageView
    private lateinit var iconHistory: ImageView
    private lateinit var iconAbout: ImageView
    private lateinit var iconSettings: ImageView

    private lateinit var textHome: TextView
    private lateinit var textSearch: TextView
    private lateinit var textHistory: TextView
    private lateinit var textAbout: TextView
    private lateinit var textSettings: TextView

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
        setupBottomNav()
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
        btnPortfolio = findViewById(R.id.btnPortfolio)
        layoutCategoryChips = findViewById(R.id.layoutCategoryChips)
        tvMarketStatusBanner = findViewById(R.id.tvMarketStatusBanner)
        rvMarketInstruments = findViewById(R.id.rvMarketInstruments)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyStateTitle = findViewById(R.id.tvEmptyStateTitle)
        tvEmptyStateSubtitle = findViewById(R.id.tvEmptyStateSubtitle)

        // Bottom Nav
        tabHome = findViewById(R.id.tabHome)
        tabSearch = findViewById(R.id.tabSearch)
        tabHistory = findViewById(R.id.tabHistory)
        tabAbout = findViewById(R.id.tabAbout)
        tabSettings = findViewById(R.id.tabSettings)

        pillHome = findViewById(R.id.pillHome)
        pillSearch = findViewById(R.id.pillSearch)
        pillHistory = findViewById(R.id.pillHistory)
        pillAbout = findViewById(R.id.pillAbout)
        pillSettings = findViewById(R.id.pillSettings)

        iconHome = findViewById(R.id.iconHome)
        iconSearch = findViewById(R.id.iconSearch)
        iconHistory = findViewById(R.id.iconHistory)
        iconAbout = findViewById(R.id.iconAbout)
        iconSettings = findViewById(R.id.iconSettings)

        textHome = findViewById(R.id.textHome)
        textSearch = findViewById(R.id.textSearch)
        textHistory = findViewById(R.id.textHistory)
        textAbout = findViewById(R.id.textAbout)
        textSettings = findViewById(R.id.textSettings)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContentContainer)) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val layoutToolbar = findViewById<LinearLayout>(R.id.layoutToolbar)
            layoutToolbar.setPadding(
                layoutToolbar.paddingLeft,
                statusBarHeight,
                layoutToolbar.paddingRight,
                layoutToolbar.paddingBottom
            )

            val bottomNav = findViewById<View>(R.id.bottomNav)
            val lp = bottomNav.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                val density = resources.displayMetrics.density
                lp.bottomMargin = navBarHeight + (12 * density).toInt()
                bottomNav.layoutParams = lp
            }

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
                viewModel.setCategory(this, category)
                highlightCategoryChip(category)
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

        viewModel.loadMarket(this)
    }

    private fun setupListeners() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnPortfolio.setOnClickListener {
            val intent = Intent(this, PortfolioActivity::class.java)
            ActivityTransition.startActivitySmooth(this, intent)
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

    private fun setupBottomNav() {
        unselectAllBottomNavTabs()

        tabHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }
    }

    private fun unselectAllBottomNavTabs() {
        val tabs = listOf(
            Triple(pillHome, iconHome, textHome),
            Triple(pillSearch, iconSearch, textSearch),
            Triple(pillHistory, iconHistory, textHistory),
            Triple(pillAbout, iconAbout, textAbout),
            Triple(pillSettings, iconSettings, textSettings),
        )
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)
        tabs.forEach { (pill, icon, text) ->
            pill.setBackgroundColor(Color.TRANSPARENT)
            text.visibility = View.GONE
            icon.setColorFilter(secondaryColor)
        }
    }

    override fun onResume() {
        super.onResume()
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
