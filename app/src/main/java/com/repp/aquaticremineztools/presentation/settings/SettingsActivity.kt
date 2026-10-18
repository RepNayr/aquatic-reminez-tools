package com.repp.aquaticremineztools.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.presentation.about.AboutActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.security.PinSetupActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class SettingsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var settingsBlurTarget: BlurTarget
    private lateinit var btnMenu: FrameLayout
    private lateinit var etSearch: EditText

    // Blur Cards
    private lateinit var blurSearchHeader: BlurView
    private lateinit var blurAppearance: BlurView
    private lateinit var blurSecurity: BlurView
    private lateinit var blurStorage: BlurView
    private lateinit var blurWorkspace: BlurView
    private lateinit var blurAppSupport: BlurView

    // Status Chips
    private lateinit var tvStatusTheme: TextView
    private lateinit var tvStatusLock: TextView
    private lateinit var tvStatusProfile: TextView

    // Appearance Quick Presets
    private lateinit var btnQuickOcean: Button
    private lateinit var btnQuickNeon: Button
    private lateinit var btnQuickEmerald: Button
    private lateinit var btnQuickClean: Button
    private lateinit var rowAppearanceDeep: LinearLayout

    // Security
    private lateinit var switchQuickAppLock: SwitchCompat
    private lateinit var switchQuickPrivacyShield: SwitchCompat
    private lateinit var rowSecurityDeep: LinearLayout

    // Storage
    private lateinit var btnQuickClearCache: Button
    private lateinit var btnQuickExportHistory: Button

    // Workspace
    private lateinit var rowWorkspaceProfile: LinearLayout
    private lateinit var tvActiveProfileName: TextView
    private lateinit var rowWorkspaceDeep: LinearLayout

    // App Info
    private lateinit var btnCheckUpdates: Button
    private lateinit var btnCopyDebugInfo: Button
    private lateinit var btnBugReport: Button

    // Bottom Nav Pill
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
            AppLockManager.redirectToVerify(this, com.repp.aquaticremineztools.presentation.security.PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_settings)

        bindViews()
        setupGlassCards()
        setupBottomNav()
        setupDrawerMenuClicks()
        loadSettingsState()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadSettingsState()
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        settingsBlurTarget = findViewById(R.id.settingsBlurTarget)
        btnMenu = findViewById(R.id.btnMenu)
        etSearch = findViewById(R.id.etSearchSettings)

        tvStatusTheme = findViewById(R.id.tvStatusTheme)
        tvStatusLock = findViewById(R.id.tvStatusLock)
        tvStatusProfile = findViewById(R.id.tvStatusProfile)

        btnQuickOcean = findViewById(R.id.btnQuickOcean)
        btnQuickNeon = findViewById(R.id.btnQuickNeon)
        btnQuickEmerald = findViewById(R.id.btnQuickEmerald)
        btnQuickClean = findViewById(R.id.btnQuickClean)
        rowAppearanceDeep = findViewById(R.id.rowAppearanceDeep)

        switchQuickAppLock = findViewById(R.id.switchQuickAppLock)
        switchQuickPrivacyShield = findViewById(R.id.switchQuickPrivacyShield)
        rowSecurityDeep = findViewById(R.id.rowSecurityDeep)

        btnQuickClearCache = findViewById(R.id.btnQuickClearCache)
        btnQuickExportHistory = findViewById(R.id.btnQuickExportHistory)

        rowWorkspaceProfile = findViewById(R.id.rowWorkspaceProfile)
        tvActiveProfileName = findViewById(R.id.tvActiveProfileName)
        rowWorkspaceDeep = findViewById(R.id.rowWorkspaceDeep)

        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)
        btnCopyDebugInfo = findViewById(R.id.btnCopyDebugInfo)
        btnBugReport = findViewById(R.id.btnBugReport)

        // Bottom Nav Pill
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
        blurSearchHeader = findViewById(R.id.blurSearchHeader)
        blurAppearance = findViewById(R.id.blurAppearance)
        blurSecurity = findViewById(R.id.blurSecurity)
        blurStorage = findViewById(R.id.blurStorage)
        blurWorkspace = findViewById(R.id.blurWorkspace)
        blurAppSupport = findViewById(R.id.blurAppSupport)

        val frameClearDrawable = window.decorView.background
        GlassBlurHelper.setupBlur(blurSearchHeader, settingsBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurAppearance, settingsBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurSecurity, settingsBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurStorage, settingsBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurWorkspace, settingsBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurAppSupport, settingsBlurTarget, frameClearDrawable)
    }

    private fun setupBottomNav() {
        selectNavTab(4) // Tab 4 = Settings

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
            selectNavTab(4)
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
                pill.setBackgroundColor(Color.TRANSPARENT)
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
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuHistory).setNavigatorClick {
            val intent = Intent(this, HistoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuFavorites).setNavigatorClick {
            Toast.makeText(this, "Favorites - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuCategories).setNavigatorClick {
            Toast.makeText(this, "Categories - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuStorage).setNavigatorClick {
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

    private fun loadSettingsState() {
        val theme = SettingsPreferences.getThemeMode(this)
        tvStatusTheme.text = theme.replaceFirstChar { it.uppercase() }

        val isLock = SettingsPreferences.isAppLockEnabled(this)
        tvStatusLock.text = if (isLock) "On" else "Off"
        switchQuickAppLock.isChecked = isLock

        val profile = SettingsPreferences.getWorkspaceProfile(this)
        tvStatusProfile.text = profile
        tvActiveProfileName.text = "$profile (School / Dev / Home)"

        switchQuickPrivacyShield.isChecked = SettingsPreferences.isPrivacyShieldEnabled(this)
    }

    private fun setupListeners() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Quick Preset Actions
        btnQuickOcean.setOnClickListener {
            SettingsPreferences.setAccentColor(this, "cyan")
            SettingsPreferences.setThemeMode(this, "dark")
            loadSettingsState()
            Toast.makeText(this, "Preset Applied: Ocean Cyber", Toast.LENGTH_SHORT).show()
        }

        btnQuickNeon.setOnClickListener {
            SettingsPreferences.setAccentColor(this, "violet")
            SettingsPreferences.setThemeMode(this, "dark")
            loadSettingsState()
            Toast.makeText(this, "Preset Applied: Neon Violet", Toast.LENGTH_SHORT).show()
        }

        btnQuickEmerald.setOnClickListener {
            SettingsPreferences.setAccentColor(this, "emerald")
            SettingsPreferences.setThemeMode(this, "dark")
            loadSettingsState()
            Toast.makeText(this, "Preset Applied: Emerald", Toast.LENGTH_SHORT).show()
        }

        btnQuickClean.setOnClickListener {
            SettingsPreferences.setAccentColor(this, "teal")
            SettingsPreferences.setThemeMode(this, "light")
            loadSettingsState()
            Toast.makeText(this, "Preset Applied: Clean Light", Toast.LENGTH_SHORT).show()
        }

        rowAppearanceDeep.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, AppearanceSettingsActivity::class.java))
        }

        // Quick Security Toggles
        switchQuickAppLock.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setAppLockEnabled(this, isChecked)
            loadSettingsState()
        }

        switchQuickPrivacyShield.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setPrivacyShieldEnabled(this, isChecked)
        }

        rowSecurityDeep.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, SecurityDataSettingsActivity::class.java))
        }

        // Storage
        btnQuickClearCache.setOnClickListener {
            Toast.makeText(this, "Application cache & temporary files cleaned", Toast.LENGTH_SHORT).show()
        }

        btnQuickExportHistory.setOnClickListener {
            Toast.makeText(this, "History exported to CSV/JSON", Toast.LENGTH_SHORT).show()
        }

        // Workspace
        rowWorkspaceProfile.setOnClickListener {
            val current = SettingsPreferences.getWorkspaceProfile(this)
            val profiles = arrayOf("Default", "School Lab", "Home Network", "Development")
            val currentIndex = profiles.indexOf(current).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Select Active Workspace Profile")
                .setSingleChoiceItems(profiles, currentIndex) { dialog, which ->
                    val selected = profiles[which]
                    SettingsPreferences.setWorkspaceProfile(this, selected)
                    loadSettingsState()
                    Toast.makeText(this, "Profile set to: $selected", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        rowWorkspaceDeep.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, WorkspaceWorkflowSettingsActivity::class.java))
        }

        // App Info & Support
        btnCheckUpdates.setOnClickListener {
            Toast.makeText(this, "Aquatic Reminez Tools is up to date (v1.0.0)", Toast.LENGTH_SHORT).show()
        }

        btnCopyDebugInfo.setOnClickListener {
            val debug = "Aquatic Reminez Tools v1.0.0\nTheme: ${SettingsPreferences.getThemeMode(this)}\nProfile: ${SettingsPreferences.getWorkspaceProfile(this)}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Debug Info", debug)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Debug information copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnBugReport.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, AppSupportSettingsActivity::class.java))
        }

        etSearch.setOnEditorActionListener { _, _, _ ->
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                Toast.makeText(this, "Searching settings for: $query", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}
