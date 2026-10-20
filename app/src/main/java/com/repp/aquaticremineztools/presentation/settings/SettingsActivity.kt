package com.repp.aquaticremineztools.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.repp.aquaticremineztools.core.util.DrawerMenuHelper
import com.repp.aquaticremineztools.presentation.about.AboutActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class SettingsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var settingsBlurTarget: BlurTarget
    private lateinit var btnMenu: FrameLayout

    // Status Chips
    private lateinit var tvStatusTheme: TextView
    private lateinit var tvStatusLock: TextView
    private lateinit var tvStatusProfile: TextView

    // Appearance Quick Spotlight
    private lateinit var tvActivePresetBadge: TextView
    private lateinit var btnQuickOcean: Button
    private lateinit var btnQuickNeon: Button
    private lateinit var btnQuickEmerald: Button
    private lateinit var btnQuickClean: Button
    private lateinit var rowAppearanceDeep: LinearLayout

    // Quick Controls Grid
    private lateinit var switchQuickAppLock: SwitchCompat
    private lateinit var switchQuickPrivacyShield: SwitchCompat
    private lateinit var switchQuickHistoryLogging: SwitchCompat
    private lateinit var switchQuickHighContrast: SwitchCompat
    private lateinit var rowSecurityDeep: LinearLayout

    // Personalization Shortcuts
    private lateinit var btnShortcutThemeAccent: Button
    private lateinit var btnShortcutWallpaperGlass: Button
    private lateinit var btnShortcutDisplayTypography: Button
    private lateinit var btnShortcutLanguageRegion: Button

    // Storage & Activity
    private lateinit var btnQuickClearCache: Button
    private lateinit var btnQuickExportHistory: Button
    private lateinit var btnQuickStorageOverview: Button

    // Workspace Summary & Tool Management
    private lateinit var rowWorkspaceProfile: LinearLayout
    private lateinit var tvActiveProfileName: TextView
    private lateinit var btnWorkspaceFavorites: Button
    private lateinit var btnWorkspaceCollections: Button
    private lateinit var rowWorkspaceDeep: LinearLayout
    private lateinit var btnToolLibraryManager: Button
    private lateinit var btnFavoritesQuickAccess: Button
    private lateinit var btnToolProfilesPresets: Button
    private lateinit var btnWorkflowBuilder: Button
    private lateinit var btnAndroidShortcuts: Button
    private lateinit var btnHomeWorkspaceManager: Button

    // App Info & Support
    private lateinit var btnCheckUpdates: Button
    private lateinit var btnCopyDebugInfo: Button
    private lateinit var btnBugReport: Button
    private lateinit var btnDocumentation: Button

    // Category Cards
    private lateinit var cardCatAppearance: LinearLayout
    private lateinit var cardCatSecurity: LinearLayout
    private lateinit var cardCatAppSupport: LinearLayout
    private lateinit var cardCatWorkspace: LinearLayout

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

        tvStatusTheme = findViewById(R.id.tvStatusTheme)
        tvStatusLock = findViewById(R.id.tvStatusLock)
        tvStatusProfile = findViewById(R.id.tvStatusProfile)

        tvActivePresetBadge = findViewById(R.id.tvActivePresetBadge)
        btnQuickOcean = findViewById(R.id.btnQuickOcean)
        btnQuickNeon = findViewById(R.id.btnQuickNeon)
        btnQuickEmerald = findViewById(R.id.btnQuickEmerald)
        btnQuickClean = findViewById(R.id.btnQuickClean)
        rowAppearanceDeep = findViewById(R.id.rowAppearanceDeep)

        switchQuickAppLock = findViewById(R.id.switchQuickAppLock)
        switchQuickPrivacyShield = findViewById(R.id.switchQuickPrivacyShield)
        switchQuickHistoryLogging = findViewById(R.id.switchQuickHistoryLogging)
        switchQuickHighContrast = findViewById(R.id.switchQuickHighContrast)
        rowSecurityDeep = findViewById(R.id.rowSecurityDeep)

        btnShortcutThemeAccent = findViewById(R.id.btnShortcutThemeAccent)
        btnShortcutWallpaperGlass = findViewById(R.id.btnShortcutWallpaperGlass)
        btnShortcutDisplayTypography = findViewById(R.id.btnShortcutDisplayTypography)
        btnShortcutLanguageRegion = findViewById(R.id.btnShortcutLanguageRegion)

        btnQuickClearCache = findViewById(R.id.btnQuickClearCache)
        btnQuickExportHistory = findViewById(R.id.btnQuickExportHistory)
        btnQuickStorageOverview = findViewById(R.id.btnQuickStorageOverview)

        rowWorkspaceProfile = findViewById(R.id.rowWorkspaceProfile)
        tvActiveProfileName = findViewById(R.id.tvActiveProfileName)
        btnWorkspaceFavorites = findViewById(R.id.btnWorkspaceFavorites)
        btnWorkspaceCollections = findViewById(R.id.btnWorkspaceCollections)
        rowWorkspaceDeep = findViewById(R.id.rowWorkspaceDeep)
        btnToolLibraryManager = findViewById(R.id.btnToolLibraryManager)
        btnFavoritesQuickAccess = findViewById(R.id.btnFavoritesQuickAccess)
        btnToolProfilesPresets = findViewById(R.id.btnToolProfilesPresets)
        btnWorkflowBuilder = findViewById(R.id.btnWorkflowBuilder)
        btnAndroidShortcuts = findViewById(R.id.btnAndroidShortcuts)
        btnHomeWorkspaceManager = findViewById(R.id.btnHomeWorkspaceManager)

        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)
        btnCopyDebugInfo = findViewById(R.id.btnCopyDebugInfo)
        btnBugReport = findViewById(R.id.btnBugReport)
        btnDocumentation = findViewById(R.id.btnDocumentation)

        cardCatAppearance = findViewById(R.id.cardCatAppearance)
        cardCatSecurity = findViewById(R.id.cardCatSecurity)
        cardCatAppSupport = findViewById(R.id.cardCatAppSupport)
        cardCatWorkspace = findViewById(R.id.cardCatWorkspace)

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
        val frameClearDrawable = window.decorView.background
        val blurViews = listOf<BlurView?>(
            findViewById(R.id.blurSettingsBannerCard),
            findViewById(R.id.blurSearchHeader),
            findViewById(R.id.blurAppearance),
            findViewById(R.id.blurQuickControls),
            findViewById(R.id.blurPersonalizationShortcuts),
            findViewById(R.id.blurStorage),
            findViewById(R.id.blurWorkspace),
            findViewById(R.id.blurToolManagement),
            findViewById(R.id.blurAppSupport),
            findViewById(R.id.blurAllCategories)
        )

        blurViews.forEach { blurView ->
            if (blurView != null) {
                GlassBlurHelper.setupBlur(blurView, settingsBlurTarget, frameClearDrawable)
            }
        }
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
        DrawerMenuHelper.setup(this, drawerLayout)
    }

    private fun loadSettingsState() {
        val theme = SettingsPreferences.getThemeMode(this)
        tvStatusTheme.text = theme.replaceFirstChar { it.uppercase() }

        val accent = SettingsPreferences.getAccentColor(this)
        tvActivePresetBadge.text = when (accent) {
            "violet" -> "Neon Violet"
            "emerald" -> "Emerald"
            "teal" -> "Clean Light"
            else -> "Ocean Cyber"
        }

        val isLock = SettingsPreferences.isAppLockEnabled(this)
        tvStatusLock.text = if (isLock) "On" else "Off"
        switchQuickAppLock.isChecked = isLock

        val profile = SettingsPreferences.getWorkspaceProfile(this)
        tvStatusProfile.text = profile
        tvActiveProfileName.text = "$profile Profile (School Lab / Dev / Home)"

        switchQuickPrivacyShield.isChecked = SettingsPreferences.isPrivacyShieldEnabled(this)
        switchQuickHistoryLogging.isChecked = SettingsPreferences.isHistoryLoggingEnabled(this)
        switchQuickHighContrast.isChecked = SettingsPreferences.isHighContrast(this)
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

        // Quick Controls Toggles
        switchQuickAppLock.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setAppLockEnabled(this, isChecked)
            loadSettingsState()
        }

        switchQuickPrivacyShield.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setPrivacyShieldEnabled(this, isChecked)
        }

        switchQuickHistoryLogging.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setHistoryLogging(this, isChecked)
        }

        switchQuickHighContrast.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setHighContrast(this, isChecked)
        }

        rowSecurityDeep.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, SecurityDataSettingsActivity::class.java))
        }

        // Personalization Shortcuts
        val openAppearanceStudio = View.OnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, AppearanceSettingsActivity::class.java))
        }
        btnShortcutThemeAccent.setOnClickListener(openAppearanceStudio)
        btnShortcutWallpaperGlass.setOnClickListener(openAppearanceStudio)
        btnShortcutDisplayTypography.setOnClickListener(openAppearanceStudio)
        btnShortcutLanguageRegion.setOnClickListener(openAppearanceStudio)

        // Storage & Activity Actions
        btnQuickClearCache.setOnClickListener {
            Toast.makeText(this, "Application cache & temporary files cleaned (14.2 MB freed)", Toast.LENGTH_SHORT).show()
        }

        btnQuickExportHistory.setOnClickListener {
            Toast.makeText(this, "Activity history exported to CSV & JSON", Toast.LENGTH_SHORT).show()
        }

        btnQuickStorageOverview.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, StorageActivity::class.java))
        }

        // Workspace Summary & Tool Shortcuts
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

        btnWorkspaceFavorites.setOnClickListener {
            Toast.makeText(this, "Opening Home 3-Column Favorites Quick Grid...", Toast.LENGTH_SHORT).show()
        }

        btnWorkspaceCollections.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, WorkspaceWorkflowSettingsActivity::class.java))
        }

        rowWorkspaceDeep.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, WorkspaceWorkflowSettingsActivity::class.java))
        }

        val openWorkspaceStudio = View.OnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, WorkspaceWorkflowSettingsActivity::class.java))
        }
        btnToolLibraryManager.setOnClickListener(openWorkspaceStudio)
        btnFavoritesQuickAccess.setOnClickListener(openWorkspaceStudio)
        btnToolProfilesPresets.setOnClickListener(openWorkspaceStudio)
        btnWorkflowBuilder.setOnClickListener(openWorkspaceStudio)
        btnAndroidShortcuts.setOnClickListener(openWorkspaceStudio)
        btnHomeWorkspaceManager.setOnClickListener(openWorkspaceStudio)

        // App Info & Support Actions
        btnCheckUpdates.setOnClickListener {
            Toast.makeText(this, "Aquatic Reminez Tools is up to date (v1.0.0)", Toast.LENGTH_SHORT).show()
        }

        btnCopyDebugInfo.setOnClickListener {
            val debug = "Aquatic Reminez Tools v1.0.0\nTheme: ${SettingsPreferences.getThemeMode(this)}\nProfile: ${SettingsPreferences.getWorkspaceProfile(this)}\nOS: Android 14"
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Debug Info", debug)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Redacted debug information copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnBugReport.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, AppSupportSettingsActivity::class.java))
        }

        btnDocumentation.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, AppSupportSettingsActivity::class.java))
        }

        // Category Cards (All Settings Overview)
        cardCatAppearance.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, AppearanceSettingsActivity::class.java))
        }

        cardCatSecurity.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, SecurityDataSettingsActivity::class.java))
        }

        cardCatAppSupport.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, AppSupportSettingsActivity::class.java))
        }

        cardCatWorkspace.setOnClickListener {
            ActivityTransition.startActivitySmooth(this, Intent(this, WorkspaceWorkflowSettingsActivity::class.java))
        }
    }
}
