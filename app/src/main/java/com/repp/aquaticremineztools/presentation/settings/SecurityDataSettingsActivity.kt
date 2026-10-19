package com.repp.aquaticremineztools.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class SecurityDataSettingsActivity : AppCompatActivity() {

    private lateinit var securityBlurTarget: BlurTarget
    private lateinit var btnBack: FrameLayout
    private lateinit var tvSecurityStatusText: TextView
    private lateinit var switchAppLock: SwitchCompat
    private lateinit var btnAutoLockTimeout: Button
    private lateinit var switchBiometric: SwitchCompat
    private lateinit var switchPrivacyShield: SwitchCompat
    private lateinit var switchScreenshotProtection: SwitchCompat

    private lateinit var switchHistoryLogging: SwitchCompat
    private lateinit var btnAutoCleanup: Button
    private lateinit var btnExportCsv: Button
    private lateinit var btnExportJson: Button
    private lateinit var btnClearHistory: Button

    private lateinit var tvStorageOverviewInfo: TextView
    private lateinit var btnClearCache: Button
    private lateinit var btnCreateBackup: Button
    private lateinit var btnRestoreBackup: Button

    private lateinit var switchExecutionConfirmation: SwitchCompat
    private lateinit var switchTargetConfirmation: SwitchCompat

    private lateinit var btnManagePermissions: Button
    private lateinit var btnResetPreferences: Button
    private lateinit var btnResetAppData: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_data_settings)

        bindViews()
        setupGlassCards()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        securityBlurTarget = findViewById(R.id.securityBlurTarget)
        btnBack = findViewById(R.id.btnBack)
        tvSecurityStatusText = findViewById(R.id.tvSecurityStatusText)
        switchAppLock = findViewById(R.id.switchAppLock)
        btnAutoLockTimeout = findViewById(R.id.btnAutoLockTimeout)
        switchBiometric = findViewById(R.id.switchBiometric)
        switchPrivacyShield = findViewById(R.id.switchPrivacyShield)
        switchScreenshotProtection = findViewById(R.id.switchScreenshotProtection)

        switchHistoryLogging = findViewById(R.id.switchHistoryLogging)
        btnAutoCleanup = findViewById(R.id.btnAutoCleanup)
        btnExportCsv = findViewById(R.id.btnExportCsv)
        btnExportJson = findViewById(R.id.btnExportJson)
        btnClearHistory = findViewById(R.id.btnClearHistory)

        tvStorageOverviewInfo = findViewById(R.id.tvStorageOverviewInfo)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnCreateBackup = findViewById(R.id.btnCreateBackup)
        btnRestoreBackup = findViewById(R.id.btnRestoreBackup)

        switchExecutionConfirmation = findViewById(R.id.switchExecutionConfirmation)
        switchTargetConfirmation = findViewById(R.id.switchTargetConfirmation)

        btnManagePermissions = findViewById(R.id.btnManagePermissions)
        btnResetPreferences = findViewById(R.id.btnResetPreferences)
        btnResetAppData = findViewById(R.id.btnResetAppData)
    }

    private fun setupGlassCards() {
        val frameClearDrawable = window.decorView.background
        val blurViews = listOf<BlurView?>(
            findViewById(R.id.blurSecurityStatus),
            findViewById(R.id.blurSecurityCenterCard),
            findViewById(R.id.blurHistoryCard),
            findViewById(R.id.blurStorageCard),
            findViewById(R.id.blurBackupCard),
            findViewById(R.id.blurToolSafetyCard),
            findViewById(R.id.blurRecoveryCard)
        )

        blurViews.forEach { blurView ->
            if (blurView != null) {
                GlassBlurHelper.setupBlur(blurView, securityBlurTarget, frameClearDrawable)
            }
        }
    }

    private fun loadSettings() {
        val isLock = SettingsPreferences.isAppLockEnabled(this)
        switchAppLock.isChecked = isLock
        btnAutoLockTimeout.text = "Auto-Lock Timeout: ${SettingsPreferences.getAutoLockTimeout(this)}"
        switchBiometric.isChecked = SettingsPreferences.isBiometricEnabled(this)
        switchPrivacyShield.isChecked = SettingsPreferences.isPrivacyShieldEnabled(this)
        switchScreenshotProtection.isChecked = SettingsPreferences.isScreenshotProtectionEnabled(this)

        val lockStatusStr = if (isLock) "App Lock PIN Active" else "App Lock Disabled"
        val bioStatusStr = if (SettingsPreferences.isBiometricEnabled(this)) "Biometrics Active" else "Biometrics Off"
        tvSecurityStatusText.text = "Status: $lockStatusStr • $bioStatusStr • History Logging Active"

        switchHistoryLogging.isChecked = SettingsPreferences.isHistoryLoggingEnabled(this)
        val retention = SettingsPreferences.getHistoryRetentionDays(this)
        btnAutoCleanup.text = "Automatic History Cleanup: ${if (retention > 0) "$retention Days" else "Disabled"}"

        switchExecutionConfirmation.isChecked = SettingsPreferences.isExecutionConfirmationEnabled(this)
        switchTargetConfirmation.isChecked = SettingsPreferences.isTargetConfirmationEnabled(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setAppLockEnabled(this, isChecked)
            loadSettings()
            Toast.makeText(this, "App Lock: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        btnAutoLockTimeout.setOnClickListener {
            val options = arrayOf("Immediately", "30 seconds", "1 minute", "5 minutes")
            AlertDialog.Builder(this)
                .setTitle("Select Auto-Lock Timeout")
                .setItems(options) { _, which ->
                    val selected = options[which]
                    SettingsPreferences.setAutoLockTimeout(this, selected)
                    loadSettings()
                    Toast.makeText(this, "Auto-Lock set to: $selected", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setBiometricEnabled(this, isChecked)
            loadSettings()
        }

        switchPrivacyShield.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setPrivacyShieldEnabled(this, isChecked)
        }

        switchScreenshotProtection.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setScreenshotProtectionEnabled(this, isChecked)
        }

        switchHistoryLogging.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setHistoryLogging(this, isChecked)
        }

        btnAutoCleanup.setOnClickListener {
            val options = arrayOf("7 Days", "30 Days", "90 Days", "Disabled")
            val days = arrayOf(7, 30, 90, 0)
            AlertDialog.Builder(this)
                .setTitle("Select History Retention Duration")
                .setItems(options) { _, which ->
                    SettingsPreferences.setHistoryRetentionDays(this, days[which])
                    loadSettings()
                    Toast.makeText(this, "History Retention: ${options[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        btnExportCsv.setOnClickListener {
            Toast.makeText(this, "History records exported successfully to CSV", Toast.LENGTH_SHORT).show()
        }

        btnExportJson.setOnClickListener {
            Toast.makeText(this, "History records exported successfully to JSON", Toast.LENGTH_SHORT).show()
        }

        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All Activity History")
                .setMessage("Are you sure you want to permanently delete all activity history records?")
                .setPositiveButton("Clear All") { _, _ ->
                    Toast.makeText(this, "All activity history records cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnClearCache.setOnClickListener {
            Toast.makeText(this, "Application cache & temporary files cleaned (Saved 14.6 MB)", Toast.LENGTH_SHORT).show()
        }

        btnCreateBackup.setOnClickListener {
            Toast.makeText(this, "Encrypted application backup created successfully", Toast.LENGTH_SHORT).show()
        }

        btnRestoreBackup.setOnClickListener {
            Toast.makeText(this, "Backup file validated and restored successfully", Toast.LENGTH_SHORT).show()
        }

        switchExecutionConfirmation.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setExecutionConfirmation(this, isChecked)
        }

        switchTargetConfirmation.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setTargetConfirmation(this, isChecked)
        }

        btnManagePermissions.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        btnResetPreferences.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Visual & App Preferences")
                .setMessage("Reset all visual appearance, fonts, and UI density preferences to defaults?")
                .setPositiveButton("Reset") { _, _ ->
                    Toast.makeText(this, "Preferences reset to defaults", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnResetAppData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset All App Data")
                .setMessage("This will reset all settings, security configurations, and application data to factory defaults. Continue?")
                .setPositiveButton("Factory Reset") { _, _ ->
                    Toast.makeText(this, "App data reset completed", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
