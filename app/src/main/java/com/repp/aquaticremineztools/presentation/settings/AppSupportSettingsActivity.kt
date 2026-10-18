package com.repp.aquaticremineztools.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.SettingsPreferences

class AppSupportSettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var tvAppVersionInfo: TextView
    private lateinit var btnCopyDebug: Button
    private lateinit var switchGameSound: Switch
    private lateinit var switchGameVibration: Switch
    private lateinit var btnResetHighScores: Button
    private lateinit var btnCheckUpdates: Button
    private lateinit var btnReportBug: Button
    private lateinit var btnDocumentation: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_support_settings)

        bindViews()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvAppVersionInfo = findViewById(R.id.tvAppVersionInfo)
        btnCopyDebug = findViewById(R.id.btnCopyDebug)
        switchGameSound = findViewById(R.id.switchGameSound)
        switchGameVibration = findViewById(R.id.switchGameVibration)
        btnResetHighScores = findViewById(R.id.btnResetHighScores)
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)
        btnReportBug = findViewById(R.id.btnReportBug)
        btnDocumentation = findViewById(R.id.btnDocumentation)
    }

    private fun loadSettings() {
        switchGameSound.isChecked = SettingsPreferences.getGameSoundMode(this)
        switchGameVibration.isChecked = SettingsPreferences.getGameVibrationMode(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        switchGameSound.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setGameSoundMode(this, isChecked)
        }

        switchGameVibration.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setGameVibrationMode(this, isChecked)
        }

        btnCopyDebug.setOnClickListener {
            val debugInfo = "Aquatic Reminez Tools v1.0.0 (Build 11-10-2026)\nDeveloper: Reep Malefitz\nOS: Android\nStatus: Normal"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Debug Info", debugInfo)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnResetHighScores.setOnClickListener {
            Toast.makeText(this, "High scores and game data reset", Toast.LENGTH_SHORT).show()
        }

        btnCheckUpdates.setOnClickListener {
            Toast.makeText(this, "Application is up to date (v1.0.0)", Toast.LENGTH_SHORT).show()
        }

        btnReportBug.setOnClickListener {
            Toast.makeText(this, "Opening feedback & bug reporting channel...", Toast.LENGTH_SHORT).show()
        }

        btnDocumentation.setOnClickListener {
            Toast.makeText(this, "Opening documentation & FAQ...", Toast.LENGTH_SHORT).show()
        }
    }
}
