package com.repp.aquaticremineztools.presentation.settings

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.SettingsPreferences

class AppearanceSettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var rgTheme: RadioGroup
    private lateinit var seekGlassTransparency: SeekBar
    private lateinit var switchReduceMotion: Switch
    private lateinit var switchHighContrast: Switch
    private lateinit var btnResetAppearance: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appearance_settings)

        bindViews()
        loadCurrentSettings()
        setupListeners()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        rgTheme = findViewById(R.id.rgTheme)
        seekGlassTransparency = findViewById(R.id.seekGlassTransparency)
        switchReduceMotion = findViewById(R.id.switchReduceMotion)
        switchHighContrast = findViewById(R.id.switchHighContrast)
        btnResetAppearance = findViewById(R.id.btnResetAppearance)
    }

    private fun loadCurrentSettings() {
        val theme = SettingsPreferences.getThemeMode(this)
        when (theme) {
            "light" -> rgTheme.check(R.id.rbLight)
            "system" -> rgTheme.check(R.id.rbSystem)
            else -> rgTheme.check(R.id.rbDark)
        }

        val glass = SettingsPreferences.getGlassTransparency(this)
        seekGlassTransparency.progress = (glass * 100).toInt()

        switchReduceMotion.isChecked = SettingsPreferences.isReduceMotion(this)
        switchHighContrast.isChecked = SettingsPreferences.isHighContrast(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbLight -> "light"
                R.id.rbSystem -> "system"
                else -> "dark"
            }
            SettingsPreferences.setThemeMode(this, mode)
            Toast.makeText(this, "Theme updated: $mode", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAccentCyan).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "cyan")
            Toast.makeText(this, "Accent updated: Cyan", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentTeal).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "teal")
            Toast.makeText(this, "Accent updated: Teal", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentViolet).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "violet")
            Toast.makeText(this, "Accent updated: Violet", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentBlue).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "blue")
            Toast.makeText(this, "Accent updated: Blue", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentEmerald).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "emerald")
            Toast.makeText(this, "Accent updated: Emerald", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSelectWallpaper).setOnClickListener {
            Toast.makeText(this, "Wallpaper selection preview active", Toast.LENGTH_SHORT).show()
        }

        seekGlassTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    SettingsPreferences.setGlassTransparency(this@AppearanceSettingsActivity, progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnPresetOcean).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "cyan")
            Toast.makeText(this, "Preset applied: Ocean Cyber", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPresetNeon).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "violet")
            Toast.makeText(this, "Preset applied: Neon Violet", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPresetEmerald).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "emerald")
            Toast.makeText(this, "Preset applied: Emerald", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPresetClean).setOnClickListener {
            SettingsPreferences.setThemeMode(this, "light")
            SettingsPreferences.setAccentColor(this, "teal")
            Toast.makeText(this, "Preset applied: Clean Light", Toast.LENGTH_SHORT).show()
            loadCurrentSettings()
        }

        switchReduceMotion.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setReduceMotion(this, isChecked)
        }

        switchHighContrast.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setHighContrast(this, isChecked)
        }

        btnResetAppearance.setOnClickListener {
            SettingsPreferences.setThemeMode(this, "dark")
            SettingsPreferences.setAccentColor(this, "cyan")
            SettingsPreferences.setGlassTransparency(this, 0.75f)
            SettingsPreferences.setReduceMotion(this, false)
            SettingsPreferences.setHighContrast(this, false)
            loadCurrentSettings()
            Toast.makeText(this, "Appearance reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }
}
