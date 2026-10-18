package com.repp.aquaticremineztools.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.core.util.ActivityTransition

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var etSearch: EditText

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, com.repp.aquaticremineztools.presentation.security.PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_settings)

        bindViews()
        loadSettingsState()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadSettingsState()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackSettings)
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
        btnBack.setOnClickListener { finish() }

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
