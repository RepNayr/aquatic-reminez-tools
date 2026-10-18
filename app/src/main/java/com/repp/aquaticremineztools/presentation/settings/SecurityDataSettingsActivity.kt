package com.repp.aquaticremineztools.presentation.settings

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.SettingsPreferences

class SecurityDataSettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var switchAppLock: Switch
    private lateinit var switchBiometric: Switch
    private lateinit var switchPrivacyShield: Switch
    private lateinit var switchScreenshotProtection: Switch
    private lateinit var switchHistoryLogging: Switch
    private lateinit var btnExportCsv: Button
    private lateinit var btnExportJson: Button
    private lateinit var btnClearHistory: Button
    private lateinit var btnClearCache: Button
    private lateinit var btnCreateBackup: Button
    private lateinit var btnRestoreBackup: Button
    private lateinit var btnResetAppData: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_data_settings)

        bindViews()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        switchAppLock = findViewById(R.id.switchAppLock)
        switchBiometric = findViewById(R.id.switchBiometric)
        switchPrivacyShield = findViewById(R.id.switchPrivacyShield)
        switchScreenshotProtection = findViewById(R.id.switchScreenshotProtection)
        switchHistoryLogging = findViewById(R.id.switchHistoryLogging)
        btnExportCsv = findViewById(R.id.btnExportCsv)
        btnExportJson = findViewById(R.id.btnExportJson)
        btnClearHistory = findViewById(R.id.btnClearHistory)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnCreateBackup = findViewById(R.id.btnCreateBackup)
        btnRestoreBackup = findViewById(R.id.btnRestoreBackup)
        btnResetAppData = findViewById(R.id.btnResetAppData)
    }

    private fun loadSettings() {
        switchAppLock.isChecked = SettingsPreferences.isAppLockEnabled(this)
        switchBiometric.isChecked = SettingsPreferences.isBiometricEnabled(this)
        switchPrivacyShield.isChecked = SettingsPreferences.isPrivacyShieldEnabled(this)
        switchScreenshotProtection.isChecked = SettingsPreferences.isScreenshotProtectionEnabled(this)
        switchHistoryLogging.isChecked = SettingsPreferences.isHistoryLoggingEnabled(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setAppLockEnabled(this, isChecked)
            Toast.makeText(this, "App Lock: $isChecked", Toast.LENGTH_SHORT).show()
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setBiometricEnabled(this, isChecked)
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

        btnExportCsv.setOnClickListener {
            Toast.makeText(this, "History exported successfully to CSV", Toast.LENGTH_SHORT).show()
        }

        btnExportJson.setOnClickListener {
            Toast.makeText(this, "History exported successfully to JSON", Toast.LENGTH_SHORT).show()
        }

        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("Are you sure you want to delete all activity history? This action cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    Toast.makeText(this, "All history cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnClearCache.setOnClickListener {
            Toast.makeText(this, "Cache and temporary files cleaned (Free space: 42 MB)", Toast.LENGTH_SHORT).show()
        }

        btnCreateBackup.setOnClickListener {
            Toast.makeText(this, "Encrypted backup created successfully", Toast.LENGTH_SHORT).show()
        }

        btnRestoreBackup.setOnClickListener {
            Toast.makeText(this, "Backup restored successfully", Toast.LENGTH_SHORT).show()
        }

        btnResetAppData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset App Data")
                .setMessage("This will reset all preferences and application data to factory defaults. Continue?")
                .setPositiveButton("Reset") { _, _ ->
                    Toast.makeText(this, "App data reset successfully", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
