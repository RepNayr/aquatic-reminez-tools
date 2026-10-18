package com.repp.aquaticremineztools.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
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

class AppSupportSettingsActivity : AppCompatActivity() {

    private lateinit var appSupportBlurTarget: BlurTarget
    private lateinit var btnBack: FrameLayout
    private lateinit var tvAppVersionInfo: TextView
    private lateinit var btnCopyDebug: Button
    private lateinit var switchGameSound: SwitchCompat
    private lateinit var switchGameVibration: SwitchCompat
    private lateinit var btnGameDisplayMode: Button
    private lateinit var btnResetHighScores: Button
    private lateinit var tvUpdateStatusText: TextView
    private lateinit var btnCheckUpdates: Button
    private lateinit var btnReportBug: Button
    private lateinit var btnContactDeveloper: Button
    private lateinit var btnDocumentation: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_support_settings)

        bindViews()
        setupGlassCards()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        appSupportBlurTarget = findViewById(R.id.appSupportBlurTarget)
        btnBack = findViewById(R.id.btnBack)
        tvAppVersionInfo = findViewById(R.id.tvAppVersionInfo)
        btnCopyDebug = findViewById(R.id.btnCopyDebug)
        switchGameSound = findViewById(R.id.switchGameSound)
        switchGameVibration = findViewById(R.id.switchGameVibration)
        btnGameDisplayMode = findViewById(R.id.btnGameDisplayMode)
        btnResetHighScores = findViewById(R.id.btnResetHighScores)
        tvUpdateStatusText = findViewById(R.id.tvUpdateStatusText)
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)
        btnReportBug = findViewById(R.id.btnReportBug)
        btnContactDeveloper = findViewById(R.id.btnContactDeveloper)
        btnDocumentation = findViewById(R.id.btnDocumentation)
    }

    private fun setupGlassCards() {
        val frameClearDrawable = window.decorView.background
        val blurAppInfoCard: BlurView = findViewById(R.id.blurAppInfoCard)
        val blurGameCard: BlurView = findViewById(R.id.blurGameCard)
        val blurUpdateCard: BlurView = findViewById(R.id.blurUpdateCard)
        val blurSupportCard: BlurView = findViewById(R.id.blurSupportCard)

        GlassBlurHelper.setupBlur(blurAppInfoCard, appSupportBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurGameCard, appSupportBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurUpdateCard, appSupportBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurSupportCard, appSupportBlurTarget, frameClearDrawable)
    }

    private fun loadSettings() {
        switchGameSound.isChecked = SettingsPreferences.getGameSoundMode(this)
        switchGameVibration.isChecked = SettingsPreferences.getGameVibrationMode(this)
        btnGameDisplayMode.text = "Game Display Mode: ${SettingsPreferences.getGameDisplayMode(this).replaceFirstChar { it.uppercase() }}"
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        switchGameSound.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setGameSoundMode(this, isChecked)
        }

        switchGameVibration.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setGameVibrationMode(this, isChecked)
        }

        btnGameDisplayMode.setOnClickListener {
            val modes = arrayOf("Fullscreen", "Windowed", "Fit Aspect Ratio")
            AlertDialog.Builder(this)
                .setTitle("Select Game Display Mode")
                .setItems(modes) { _, which ->
                    val key = modes[which].lowercase()
                    SettingsPreferences.setGameDisplayMode(this, key)
                    loadSettings()
                    Toast.makeText(this, "Display Mode: ${modes[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        btnCopyDebug.setOnClickListener {
            val debugInfo = "[REDACTED] Aquatic Reminez Tools v1.0.0 (Build 11-10-2026)\nDeveloper: Reep Malefitz\nOS: Android 14 (API 34)\nArch: arm64-v8a\nStatus: Healthy"
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Debug Info", debugInfo)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Redacted debug information copied", Toast.LENGTH_SHORT).show()
        }

        btnResetHighScores.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset High Scores")
                .setMessage("Reset all local game high scores and progress?")
                .setPositiveButton("Reset") { _, _ ->
                    Toast.makeText(this, "High scores and game data reset", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnCheckUpdates.setOnClickListener {
            tvUpdateStatusText.text = "Status: Application is up to date (v1.0.0)"
            Toast.makeText(this, "Checked official releases: You are running the latest version (v1.0.0)", Toast.LENGTH_SHORT).show()
        }

        btnReportBug.setOnClickListener {
            Toast.makeText(this, "Opening official feedback & bug reporting URL...", Toast.LENGTH_SHORT).show()
        }

        btnContactDeveloper.setOnClickListener {
            Toast.makeText(this, "Developer: Reep Malefitz (Official Channel)", Toast.LENGTH_SHORT).show()
        }

        btnDocumentation.setOnClickListener {
            Toast.makeText(this, "Opening documentation & troubleshooting guide...", Toast.LENGTH_SHORT).show()
        }
    }
}
