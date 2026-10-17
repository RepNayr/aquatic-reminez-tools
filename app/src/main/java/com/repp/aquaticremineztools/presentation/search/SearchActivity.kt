package com.repp.aquaticremineztools.presentation.search

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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.history.HistoryLogger
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.data.ToolData
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import com.repp.aquaticremineztools.presentation.about.AboutActivity
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class SearchActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var homeBlurTarget: BlurTarget
    private lateinit var blurSearchBar: BlurView
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var btnMenu: FrameLayout
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var cgCategories: ChipGroup
    private lateinit var tvResultCount: TextView

    private lateinit var adapter: SearchResultAdapter

    // Bottom Nav
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

    private var selectedCategories: Set<ToolData.Category>? = null
    private var currentQuery = ""

    private data class SearchCategoryChip(
        val label: String,
        val categories: Set<ToolData.Category>?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_search)

        bindViews()
        setupGlassCards()
        setupBottomNav()
        setupDrawerMenuClicks()
        setupCategoryChips()
        setupRecyclerView()
        setupListeners()

        etSearch.requestFocus()
        updateResults()
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        homeBlurTarget = findViewById(R.id.homeBlurTarget)
        blurSearchBar = findViewById(R.id.blurSearchBar)
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnMenu = findViewById(R.id.btnMenu)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        emptyState = findViewById(R.id.emptyState)
        cgCategories = findViewById(R.id.cgCategories)
        tvResultCount = findViewById(R.id.tvResultCount)

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

    private fun setupGlassCards() {
        val frameClearDrawable = window.decorView.background
        GlassBlurHelper.setupBlur(blurSearchBar, homeBlurTarget, frameClearDrawable)
    }

    private fun setupCategoryChips() {
        val context = this
        cgCategories.setOnCheckedStateChangeListener(null)
        cgCategories.removeAllViews()

        val chipItems = listOf(
            SearchCategoryChip("All Tools", null),
            SearchCategoryChip("🔧 Utility", setOf(ToolData.Category.UTILITY)),
            SearchCategoryChip("🔐 Security & Generate", setOf(ToolData.Category.SECURITY_GEN)),
            SearchCategoryChip("📁 File & System", setOf(ToolData.Category.FILE)),
            SearchCategoryChip("🌐 Cyber Tools", setOf(ToolData.Category.NETWORK, ToolData.Category.OFFENSIVE, ToolData.Category.DEFENSIVE)),
            SearchCategoryChip("🎨 Media", setOf(ToolData.Category.MEDIA)),
            SearchCategoryChip("🧰 Dev Tools", setOf(ToolData.Category.DEV)),
            SearchCategoryChip("🔍 Forensic", setOf(ToolData.Category.FORENSIC)),
            SearchCategoryChip("🛡️ Security", setOf(ToolData.Category.SECURITY)),
            SearchCategoryChip("🎮 Games", setOf(ToolData.Category.GAMES)),
        )

        chipItems.forEachIndexed { index, item ->
            val chip = Chip(context).apply {
                id = View.generateViewId()
                text = item.label
                tag = item.categories
                isCheckable = true
                isChecked = index == 0
                setChipBackgroundColorResource(R.color.bg_card)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            cgCategories.addView(chip)
        }

        cgCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val chip = if (checkedId != null) group.findViewById<Chip>(checkedId) else null
            @Suppress("UNCHECKED_CAST")
            selectedCategories = chip?.tag as? Set<ToolData.Category>
            updateResults()
        }
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter(
            blurTarget = homeBlurTarget,
            frameClearDrawable = window.decorView.background,
            results = emptyList()
        ) { tool ->
            Toast.makeText(this, "Opening ${tool.name}...", Toast.LENGTH_SHORT).show()
            HistoryLogger.logOpened(this, tool.id, tool.name, tool.category.displayName)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = adapter
    }

    private fun setupListeners() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                updateResults()
            }
        })
    }

    private fun updateResults() {
        var results = ToolData.searchTools(currentQuery)
        val categories = selectedCategories
        if (!categories.isNullOrEmpty()) {
            results = results.filter { it.category in categories }
        }

        adapter.updateResults(results)
        tvResultCount.text = "${results.size} tools"

        if (results.isEmpty()) {
            rvSearchResults.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvSearchResults.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun setupBottomNav() {
        selectNavTab(1)

        tabHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabSearch.setOnClickListener {
            selectNavTab(1)
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
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectNavTab(index: Int) {
        val tabContainers = listOf(tabHome, tabSearch, tabHistory, tabAbout, tabSettings)
        val tabs = listOf(
            Triple(pillHome, iconHome, textHome),
            Triple(pillSearch, iconSearch, textSearch),
            Triple(pillHistory, iconHistory, textHistory),
            Triple(pillAbout, iconAbout, textAbout),
            Triple(pillSettings, iconSettings, textSettings),
        )

        val primaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        tabs.forEachIndexed { i, (pill, icon, text) ->
            val container = tabContainers[i]
            val lp = container.layoutParams as? LinearLayout.LayoutParams
            if (i == index) {
                lp?.weight = 1.4f
                container.layoutParams = lp
                container.requestLayout()
                pill.setBackgroundResource(R.drawable.bg_nav_active_pill)
                text.visibility = View.VISIBLE
                icon.setColorFilter(primaryColor)
            } else {
                lp?.weight = 0.9f
                container.layoutParams = lp
                container.requestLayout()
                pill.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                text.visibility = View.GONE
                icon.setColorFilter(secondaryColor)
            }
        }
    }

    private fun setupDrawerMenuClicks() {
        findViewById<LinearLayout>(R.id.menuHome).setNavigatorClick {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuSearch).setNavigatorClick {
            closeDrawer()
        }

        findViewById<LinearLayout>(R.id.menuHistory).setNavigatorClick {
            closeDrawer()
            val intent = Intent(this, HistoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuFavorites).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Favorites - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuCategories).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Categories - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuStorage).setNavigatorClick {
            closeDrawer()
            ActivityTransition.startActivitySmooth(this, Intent(this, StorageActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.menuAbout).setNavigatorClick {
            val intent = Intent(this, AboutActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuSettings).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun View.setNavigatorClick(action: () -> Unit) {
        setOnClickListener {
            closeDrawer()
            action()
        }
    }

    private fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectNavTab(1)
    }

    override fun onResume() {
        super.onResume()
        selectNavTab(1)
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
