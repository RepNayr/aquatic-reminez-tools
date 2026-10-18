package com.repp.aquaticremineztools.presentation.settings

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class AppearanceSettingsActivity : AppCompatActivity() {

    private lateinit var appearanceBlurTarget: BlurTarget
    private lateinit var btnBack: FrameLayout
    private lateinit var rgTheme: RadioGroup
    private lateinit var seekGlassTransparency: SeekBar
    private lateinit var seekBlurIntensity: SeekBar
    private lateinit var switchReduceMotion: SwitchCompat
    private lateinit var switchHighContrast: SwitchCompat
    private lateinit var switchLightweightEffects: SwitchCompat
    private lateinit var btnResetAppearance: Button

    private lateinit var btnCustomColorPicker: Button
    private lateinit var btnFontSize: Button
    private lateinit var btnUiDensity: Button
    private lateinit var btnCardStyle: Button
    private lateinit var btnLanguage: Button
    private lateinit var btnDateTimeFormats: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appearance_settings)

        bindViews()
        setupGlassCards()
        loadCurrentSettings()
        setupListeners()
    }

    private fun bindViews() {
        appearanceBlurTarget = findViewById(R.id.appearanceBlurTarget)
        btnBack = findViewById(R.id.btnBack)
        rgTheme = findViewById(R.id.rgTheme)
        seekGlassTransparency = findViewById(R.id.seekGlassTransparency)
        seekBlurIntensity = findViewById(R.id.seekBlurIntensity)
        switchReduceMotion = findViewById(R.id.switchReduceMotion)
        switchHighContrast = findViewById(R.id.switchHighContrast)
        switchLightweightEffects = findViewById(R.id.switchLightweightEffects)
        btnResetAppearance = findViewById(R.id.btnResetAppearance)

        btnCustomColorPicker = findViewById(R.id.btnCustomColorPicker)
        btnFontSize = findViewById(R.id.btnFontSize)
        btnUiDensity = findViewById(R.id.btnUiDensity)
        btnCardStyle = findViewById(R.id.btnCardStyle)
        btnLanguage = findViewById(R.id.btnLanguage)
        btnDateTimeFormats = findViewById(R.id.btnDateTimeFormats)
    }

    private fun setupGlassCards() {
        val frameClearDrawable = window.decorView.background
        val blurPreviewCard: BlurView = findViewById(R.id.blurPreviewCard)
        val blurThemeCard: BlurView = findViewById(R.id.blurThemeCard)
        val blurWallpaperCard: BlurView = findViewById(R.id.blurWallpaperCard)
        val blurDisplayCard: BlurView = findViewById(R.id.blurDisplayCard)
        val blurLanguageCard: BlurView = findViewById(R.id.blurLanguageCard)
        val blurAccessibilityCard: BlurView = findViewById(R.id.blurAccessibilityCard)
        val blurPresetsCard: BlurView = findViewById(R.id.blurPresetsCard)

        GlassBlurHelper.setupBlur(blurPreviewCard, appearanceBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurThemeCard, appearanceBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurWallpaperCard, appearanceBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurDisplayCard, appearanceBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurLanguageCard, appearanceBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurAccessibilityCard, appearanceBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurPresetsCard, appearanceBlurTarget, frameClearDrawable)
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

        val blur = SettingsPreferences.getBlurIntensity(this)
        seekBlurIntensity.progress = blur

        switchReduceMotion.isChecked = SettingsPreferences.isReduceMotion(this)
        switchHighContrast.isChecked = SettingsPreferences.isHighContrast(this)
        switchLightweightEffects.isChecked = SettingsPreferences.isLightweightEffects(this)

        btnFontSize.text = "Font Size: ${SettingsPreferences.getFontSize(this).replaceFirstChar { it.uppercase() }}"
        btnUiDensity.text = "UI Density: ${SettingsPreferences.getUiDensity(this).replaceFirstChar { it.uppercase() }}"
        btnCardStyle.text = "Card Style: ${SettingsPreferences.getCardStyle(this).replaceFirstChar { it.uppercase() }}"

        val lang = if (SettingsPreferences.getLanguage(this) == "id") "Bahasa Indonesia" else "English"
        btnLanguage.text = "Language: $lang"
        btnDateTimeFormats.text = "Date Format: ${SettingsPreferences.getDateFormat(this)} • ${SettingsPreferences.getTimeFormat(this)}"
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
            Toast.makeText(this, "Accent: Cyan", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentTeal).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "teal")
            Toast.makeText(this, "Accent: Teal", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentViolet).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "violet")
            Toast.makeText(this, "Accent: Violet", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentBlue).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "blue")
            Toast.makeText(this, "Accent: Blue", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAccentEmerald).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "emerald")
            Toast.makeText(this, "Accent: Emerald", Toast.LENGTH_SHORT).show()
        }

        btnCustomColorPicker.setOnClickListener {
            val colors = arrayOf("Cyan (#43D9E6)", "Teal (#38BFA1)", "Violet (#A78BFA)", "Gold (#D6B46A)", "Red (#D9535D)")
            AlertDialog.Builder(this)
                .setTitle("Select Custom Accent Color")
                .setItems(colors) { _, which ->
                    val colorHex = when(which) {
                        1 -> "#38BFA1"
                        2 -> "#A78BFA"
                        3 -> "#D6B46A"
                        4 -> "#D9535D"
                        else -> "#43D9E6"
                    }
                    SettingsPreferences.setCustomAccentHex(this, colorHex)
                    Toast.makeText(this, "Custom Accent Hex: $colorHex", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        findViewById<Button>(R.id.btnSelectWallpaper).setOnClickListener {
            Toast.makeText(this, "Wallpaper selection active (Built-in / Gallery)", Toast.LENGTH_SHORT).show()
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

        seekBlurIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    SettingsPreferences.setBlurIntensity(this@AppearanceSettingsActivity, progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnFontSize.setOnClickListener {
            val sizes = arrayOf("Small", "Default", "Large", "Extra Large")
            AlertDialog.Builder(this)
                .setTitle("Select Font Size")
                .setSingleChoiceItems(sizes, 1) { dialog, which ->
                    val sizeKey = sizes[which].lowercase()
                    SettingsPreferences.setFontSize(this, sizeKey)
                    loadCurrentSettings()
                    Toast.makeText(this, "Font Size set to ${sizes[which]}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }

        btnUiDensity.setOnClickListener {
            val densities = arrayOf("Compact", "Comfortable")
            AlertDialog.Builder(this)
                .setTitle("Select UI Density")
                .setSingleChoiceItems(densities, 1) { dialog, which ->
                    val key = densities[which].lowercase()
                    SettingsPreferences.setUiDensity(this, key)
                    loadCurrentSettings()
                    Toast.makeText(this, "UI Density set to ${densities[which]}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }

        btnCardStyle.setOnClickListener {
            val styles = arrayOf("Standard", "Glass")
            AlertDialog.Builder(this)
                .setTitle("Select Card Style")
                .setSingleChoiceItems(styles, 1) { dialog, which ->
                    val key = styles[which].lowercase()
                    SettingsPreferences.setCardStyle(this, key)
                    loadCurrentSettings()
                    Toast.makeText(this, "Card Style set to ${styles[which]}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }

        btnLanguage.setOnClickListener {
            val languages = arrayOf("English", "Bahasa Indonesia")
            AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, if (SettingsPreferences.getLanguage(this) == "id") 1 else 0) { dialog, which ->
                    val langKey = if (which == 1) "id" else "en"
                    SettingsPreferences.setLanguage(this, langKey)
                    loadCurrentSettings()
                    Toast.makeText(this, "Language set to ${languages[which]}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }

        btnDateTimeFormats.setOnClickListener {
            val dateFmts = arrayOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd")
            AlertDialog.Builder(this)
                .setTitle("Select Date Format")
                .setSingleChoiceItems(dateFmts, 0) { dialog, which ->
                    SettingsPreferences.setDateFormat(this, dateFmts[which])
                    loadCurrentSettings()
                    Toast.makeText(this, "Date Format: ${dateFmts[which]}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }

        switchReduceMotion.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setReduceMotion(this, isChecked)
        }

        switchHighContrast.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setHighContrast(this, isChecked)
        }

        switchLightweightEffects.setOnCheckedChangeListener { _, isChecked ->
            SettingsPreferences.setLightweightEffects(this, isChecked)
        }

        findViewById<Button>(R.id.btnPresetOcean).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "cyan")
            SettingsPreferences.setThemeMode(this, "dark")
            loadCurrentSettings()
            Toast.makeText(this, "Preset: Ocean Cyber", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPresetNeon).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "violet")
            SettingsPreferences.setThemeMode(this, "dark")
            loadCurrentSettings()
            Toast.makeText(this, "Preset: Neon Violet", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPresetEmerald).setOnClickListener {
            SettingsPreferences.setAccentColor(this, "emerald")
            SettingsPreferences.setThemeMode(this, "dark")
            loadCurrentSettings()
            Toast.makeText(this, "Preset: Emerald", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPresetClean).setOnClickListener {
            SettingsPreferences.setThemeMode(this, "light")
            SettingsPreferences.setAccentColor(this, "teal")
            loadCurrentSettings()
            Toast.makeText(this, "Preset: Clean Light", Toast.LENGTH_SHORT).show()
        }

        btnResetAppearance.setOnClickListener {
            SettingsPreferences.setThemeMode(this, "dark")
            SettingsPreferences.setAccentColor(this, "cyan")
            SettingsPreferences.setGlassTransparency(this, 0.75f)
            SettingsPreferences.setBlurIntensity(this, 15)
            SettingsPreferences.setFontSize(this, "default")
            SettingsPreferences.setUiDensity(this, "comfortable")
            SettingsPreferences.setCardStyle(this, "glass")
            SettingsPreferences.setReduceMotion(this, false)
            SettingsPreferences.setHighContrast(this, false)
            SettingsPreferences.setLightweightEffects(this, false)
            loadCurrentSettings()
            Toast.makeText(this, "Appearance reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }
}
