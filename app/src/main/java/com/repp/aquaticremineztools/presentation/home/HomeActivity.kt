package com.repp.aquaticremineztools.presentation.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.network.InfoCardHelper
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.core.settings.PrayerPreferences
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.core.util.HijriCalendarHelper
import com.repp.aquaticremineztools.data.QuickCategory
import com.repp.aquaticremineztools.data.model.CryptoAsset
import com.repp.aquaticremineztools.data.model.NewsArticle
import com.repp.aquaticremineztools.presentation.news.NewsViewModel
import com.repp.aquaticremineztools.presentation.news.NewsUiState
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.security.PinChangeActivity
import com.repp.aquaticremineztools.presentation.security.PinSetupActivity
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.about.AboutActivity
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.core.util.DrawerMenuHelper
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    // Header
    private lateinit var btnMenu: FrameLayout
    private lateinit var btnNotif: FrameLayout
    private lateinit var btnPower: FrameLayout

    // User Card
    private lateinit var videoUserCard: PlayerView
    private lateinit var tvTime: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvCreatedDate: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var ivBatteryIcon: ImageView
    private lateinit var btnMuteCard: FrameLayout
    private lateinit var ivMuteCard: ImageView
    private lateinit var tvWeatherIcon: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvLocation: TextView

    // Info Cards
    private lateinit var tvIpValue: TextView
    private lateinit var tvConnectionLabel: TextView
    private lateinit var tvConnectionValue: TextView
    private lateinit var tvPingValue: TextView

    // Glass Blur Views
    private lateinit var homeBlurTarget: BlurTarget
    private lateinit var cardIpBlur: BlurView
    private lateinit var cardConnectionBlur: BlurView
    private lateinit var cardPingBlur: BlurView

    // Prayer Schedule Widget
    private lateinit var prayerViewModel: PrayerViewModel
    private lateinit var cardPrayerBlur: BlurView
    private lateinit var ivCurrentPrayerIcon: ImageView
    private lateinit var tvCurrentPrayer: TextView
    private lateinit var tvImsakTime: TextView
    private lateinit var tvSubuhTime: TextView
    private lateinit var tvDzuhurTime: TextView
    private lateinit var tvAsharTime: TextView
    private lateinit var tvMaghribTime: TextView
    private lateinit var tvNextPrayer: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var pbPrayerProgress: ProgressBar
    private lateinit var tvPrayerStatusIndicator: TextView

    // Gregorian-Hijri Calendar Widget
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var cardCalendarBlur: BlurView
    private lateinit var rvCalendarGrid: RecyclerView
    private lateinit var tvGregorianMonth: TextView
    private lateinit var tvHijriMonth: TextView
    private lateinit var btnPrevMonth: FrameLayout
    private lateinit var btnNextMonth: FrameLayout
    private lateinit var tvDetailGregorianDate: TextView
    private lateinit var tvDetailHijriDate: TextView
    private lateinit var tvDetailJavanesePasaran: TextView
    private lateinit var tvDetailStatusBadge: TextView
    private lateinit var tvDetailObservanceDesc: TextView
    private lateinit var tvNeutralPrompt: TextView
    private lateinit var layoutActiveDetail: LinearLayout
    private lateinit var tvCalendarStatus: TextView
    private lateinit var calendarDayAdapter: CalendarDayAdapter

    // Market Widget
    private lateinit var cryptoViewModel: CryptoViewModel
    private lateinit var cardMarketBlur: BlurView
    private lateinit var layoutCryptoRows: LinearLayout
    private lateinit var tvMarketStatusIndicator: TextView
    private lateinit var tvMarketTitle: TextView

    // News Widget
    private lateinit var newsViewModel: NewsViewModel
    private var cardNewsBlur: BlurView? = null
    private lateinit var layoutNewsHeaderTitle: LinearLayout
    private lateinit var containerFeaturedArticle: FrameLayout
    private lateinit var layoutCompactArticles: LinearLayout
    private lateinit var pbNewsLoading: ProgressBar
    private lateinit var tvNewsStatusIndicator: TextView

    private val prayerCountdownHandler = Handler(Looper.getMainLooper())
    private val prayerCountdownRunnable = object : Runnable {
        override fun run() {
            if (::prayerViewModel.isInitialized) {
                prayerViewModel.tickCountdown()
            }
            prayerCountdownHandler.postDelayed(this, 1000L)
        }
    }

    private lateinit var tvPasswordLabel: TextView

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

    private var cardPlayer: ExoPlayer? = null
    private var isCardMuted = false

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            updateTodayDate()
            clockHandler.postDelayed(this, 30_000L)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                updateBatteryUI((level * 100 / scale.toFloat()).toInt())
            }
        }
    }

    private val LOCATION_PERMISSION_CODE = 1001
    private val executor = Executors.newSingleThreadExecutor()
    private var lastLocationFetch = 0L
    private val LOCATION_REFRESH_INTERVAL = 30_000L

    private val pingHandler = Handler(Looper.getMainLooper())
    private val pingRunnable = object : Runnable {
        override fun run() {
            updatePingValue()
            pingHandler.postDelayed(this, 8_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        lastLocationFetch = 0L

        prayerViewModel = ViewModelProvider(this)[PrayerViewModel::class.java]
        cryptoViewModel = ViewModelProvider(this)[CryptoViewModel::class.java]
        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        newsViewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        bindViews()
        setupGlassCards()
        setupPrayerSchedule()
        setupCalendarSection()
        setupMarketSection()
        setupNewsSection()
        setupClicks()
        setupDrawerMenuClicks()
        setupBottomNav()
        setupVideo()
        setupMuteButton()
        setupQuickMenu()
        setupInfoCards()
        startClock()
        registerBattery()
        requestLocationAndWeather()
        updatePasswordLabel()
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)

        btnMenu = findViewById(R.id.btnMenu)
        btnNotif = findViewById(R.id.btnNotif)
        btnPower = findViewById(R.id.btnPower)

        videoUserCard = findViewById(R.id.videoUserCard)
        tvTime = findViewById(R.id.tvTime)
        tvBattery = findViewById(R.id.tvBattery)
        tvCreatedDate = findViewById(R.id.tvCreatedDate)
        tvTodayDate = findViewById(R.id.tvTodayDate)
        ivBatteryIcon = findViewById(R.id.ivBatteryIcon)
        btnMuteCard = findViewById(R.id.btnMuteCard)
        ivMuteCard = findViewById(R.id.ivMuteCard)
        tvWeatherIcon = findViewById(R.id.tvWeatherIcon)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvLocation = findViewById(R.id.tvLocation)

        // Info Cards
        tvIpValue = findViewById(R.id.tvIpValue)
        tvConnectionLabel = findViewById(R.id.tvConnectionLabel)
        tvConnectionValue = findViewById(R.id.tvConnectionValue)
        tvPingValue = findViewById(R.id.tvPingValue)

        // Prayer Schedule Widget
        cardPrayerBlur = findViewById(R.id.cardPrayerSchedule)
        ivCurrentPrayerIcon = findViewById(R.id.ivCurrentPrayerIcon)
        tvCurrentPrayer = findViewById(R.id.tvCurrentPrayer)
        tvImsakTime = findViewById(R.id.tvImsakTime)
        tvSubuhTime = findViewById(R.id.tvSubuhTime)
        tvDzuhurTime = findViewById(R.id.tvDzuhurTime)
        tvAsharTime = findViewById(R.id.tvAsharTime)
        tvMaghribTime = findViewById(R.id.tvMaghribTime)
        tvNextPrayer = findViewById(R.id.tvNextPrayer)
        tvCountdown = findViewById(R.id.tvCountdown)
        pbPrayerProgress = findViewById(R.id.pbPrayerProgress)
        tvPrayerStatusIndicator = findViewById(R.id.tvPrayerStatusIndicator)

        // Calendar Widget
        cardCalendarBlur = findViewById(R.id.cardCalendar)
        rvCalendarGrid = findViewById(R.id.rvCalendarGrid)
        tvGregorianMonth = findViewById(R.id.tvGregorianMonth)
        tvHijriMonth = findViewById(R.id.tvHijriMonth)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvDetailGregorianDate = findViewById(R.id.tvDetailGregorianDate)
        tvDetailHijriDate = findViewById(R.id.tvDetailHijriDate)
        tvDetailJavanesePasaran = findViewById(R.id.tvDetailJavanesePasaran)
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge)
        tvDetailObservanceDesc = findViewById(R.id.tvDetailObservanceDesc)
        tvNeutralPrompt = findViewById(R.id.tvNeutralPrompt)
        layoutActiveDetail = findViewById(R.id.layoutActiveDetail)
        tvCalendarStatus = findViewById(R.id.tvCalendarStatus)

        // Market Widget
        cardMarketBlur = findViewById(R.id.cardMarket)
        layoutCryptoRows = findViewById(R.id.layoutCryptoRows)
        tvMarketStatusIndicator = findViewById(R.id.tvMarketStatusIndicator)
        tvMarketTitle = findViewById(R.id.tvMarketTitle)

        // News Widget
        cardNewsBlur = findViewById(R.id.cardNews)
        layoutNewsHeaderTitle = findViewById(R.id.layoutNewsHeaderTitle)
        containerFeaturedArticle = findViewById(R.id.containerFeaturedArticle)
        layoutCompactArticles = findViewById(R.id.layoutCompactArticles)
        pbNewsLoading = findViewById(R.id.pbNewsLoading)
        tvNewsStatusIndicator = findViewById(R.id.tvNewsStatusIndicator)

        tvPasswordLabel = findViewById(R.id.tvPasswordLabel)

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

    private fun setupClicks() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        btnNotif.setOnClickListener { /* TODO */ }
        btnPower.setOnClickListener { finishAffinity() }
    }

    private fun setupBottomNav() {
        selectNavTab(0)

        tabHome.setOnClickListener {
            selectNavTab(0)
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
            val intent = Intent(this, com.repp.aquaticremineztools.presentation.settings.SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
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
        DrawerMenuHelper.setup(this, drawerLayout, listOf(cardIpBlur, cardConnectionBlur, cardPingBlur, cardPrayerBlur, cardCalendarBlur, cardMarketBlur))
    }

    private fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePasswordLabel() {
        if (::tvPasswordLabel.isInitialized) {
            tvPasswordLabel.text = if (PinManager.hasPin(this)) {
                getString(R.string.drawer_change_password)
            } else {
                getString(R.string.drawer_create_password)
            }
        }
    }

    private fun showPasswordOptionsDialog() {
        val options = arrayOf(
            getString(R.string.drawer_change_password),
            getString(R.string.drawer_delete_password),
        )

        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(R.string.drawer_features)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, PinChangeActivity::class.java))
                    1 -> confirmDeletePassword()
                }
            }
            .show()
    }

    private fun confirmDeletePassword() {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(R.string.delete_pin_title)
            .setMessage(R.string.delete_pin_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                PinManager.deletePin(this)
                Toast.makeText(this, "Password deleted", Toast.LENGTH_SHORT).show()
                updatePasswordLabel()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupGlassCards() {
        homeBlurTarget = findViewById(R.id.homeBlurTarget)
        cardIpBlur = findViewById(R.id.cardIp)
        cardConnectionBlur = findViewById(R.id.cardConnection)
        cardPingBlur = findViewById(R.id.cardPing)
        cardPrayerBlur = findViewById(R.id.cardPrayerSchedule)
        cardCalendarBlur = findViewById(R.id.cardCalendar)
        cardMarketBlur = findViewById(R.id.cardMarket)
        cardNewsBlur = findViewById(R.id.cardNews)

        val frameClearDrawable = window.decorView.background

        GlassBlurHelper.setupBlur(cardIpBlur, homeBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(cardConnectionBlur, homeBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(cardPingBlur, homeBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(cardPrayerBlur, homeBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(cardCalendarBlur, homeBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(cardMarketBlur, homeBlurTarget, frameClearDrawable)
        cardNewsBlur?.let { GlassBlurHelper.setupBlur(it, homeBlurTarget, frameClearDrawable) }
    }

    // ============================
    // PRAYER SCHEDULE WIDGET
    // ============================
    private fun setupPrayerSchedule() {
        prayerViewModel.uiState.observe(this) { state ->
            when (state) {
                is PrayerUiState.Loading -> {
                    tvPrayerStatusIndicator.visibility = View.GONE
                }
                is PrayerUiState.Success -> {
                    val s = state.schedule
                    tvImsakTime.text = s.imsak
                    tvSubuhTime.text = s.subuh
                    tvDzuhurTime.text = s.dzuhur
                    tvAsharTime.text = s.ashar
                    tvMaghribTime.text = s.maghrib

                    val nextInfo = state.nextPrayerInfo
                    ivCurrentPrayerIcon.setImageResource(nextInfo.currentPrayerIconRes)
                    tvCurrentPrayer.text = nextInfo.currentPrayerName
                    tvNextPrayer.text = getString(R.string.prayer_towards_prefix, nextInfo.prayerName.uppercase(Locale.ROOT))
                    tvCountdown.text = nextInfo.countdownText

                    if (nextInfo.hasValidProgress) {
                        pbPrayerProgress.visibility = View.VISIBLE
                        pbPrayerProgress.progress = nextInfo.progressPercent
                    } else {
                        pbPrayerProgress.visibility = View.GONE
                    }

                    tvPrayerStatusIndicator.visibility = View.GONE
                }
                is PrayerUiState.Error -> {
                    if (state.cachedSchedule != null) {
                        val s = state.cachedSchedule
                        tvImsakTime.text = s.imsak
                        tvSubuhTime.text = s.subuh
                        tvDzuhurTime.text = s.dzuhur
                        tvAsharTime.text = s.ashar
                        tvMaghribTime.text = s.maghrib

                        if (state.nextPrayerInfo != null) {
                            val nextInfo = state.nextPrayerInfo
                            ivCurrentPrayerIcon.setImageResource(nextInfo.currentPrayerIconRes)
                            tvCurrentPrayer.text = nextInfo.currentPrayerName
                            tvNextPrayer.text = getString(R.string.prayer_towards_prefix, nextInfo.prayerName.uppercase(Locale.ROOT))
                            tvCountdown.text = nextInfo.countdownText

                            if (nextInfo.hasValidProgress) {
                                pbPrayerProgress.visibility = View.VISIBLE
                                pbPrayerProgress.progress = nextInfo.progressPercent
                            } else {
                                pbPrayerProgress.visibility = View.GONE
                            }
                        }
                        tvPrayerStatusIndicator.text = getString(R.string.prayer_offline_notice, s.cityName)
                        tvPrayerStatusIndicator.visibility = View.VISIBLE
                    } else {
                        tvPrayerStatusIndicator.text = getString(R.string.prayer_error_notice)
                        tvPrayerStatusIndicator.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // ============================
    // GREGORIAN-HIJRI CALENDAR WIDGET
    // ============================
    private fun setupCalendarSection() {
        rvCalendarGrid.layoutManager = GridLayoutManager(this, 7)
        calendarDayAdapter = CalendarDayAdapter { selectedDay ->
            calendarViewModel.onSelectDay(this, selectedDay)
        }
        rvCalendarGrid.adapter = calendarDayAdapter

        btnPrevMonth.setOnClickListener {
            calendarViewModel.onPrevMonth(this)
        }

        btnNextMonth.setOnClickListener {
            calendarViewModel.onNextMonth(this)
        }

        calendarViewModel.monthData.observe(this) { monthData ->
            if (::cardCalendarBlur.isInitialized) {
                android.transition.TransitionManager.beginDelayedTransition(
                    cardCalendarBlur,
                    android.transition.AutoTransition().apply { duration = 150 }
                )
            }

            tvGregorianMonth.text = monthData.gregorianMonthTitle
            tvHijriMonth.text = monthData.hijriMonthSubtitle
            tvCalendarStatus.text = monthData.statusText

            calendarDayAdapter.submitList(monthData.days)

            val selected = monthData.selectedDay
            if (selected != null) {
                layoutActiveDetail.visibility = View.VISIBLE
                tvNeutralPrompt.visibility = View.GONE

                tvDetailGregorianDate.text = selected.fullGregorianFormatted
                tvDetailHijriDate.text = selected.fullHijriFormatted
                tvDetailJavanesePasaran.text = selected.javanesePasaran

                val isEng = SettingsPreferences.getLanguage(this) != "id"
                if (!selected.observance.isNullOrEmpty()) {
                    val details = HijriCalendarHelper.getHolidayDetails(selected.observance, isEng)
                    val holidayName = details?.first ?: selected.observance
                    val holidayDesc = details?.second ?: (if (isEng) "Special national or religious observance day." else "Hari libur atau peringatan khusus.")

                    val lower = selected.observance.lowercase()
                    val isPublicHoliday = lower.contains("kemerdekaan") || lower.contains("independence") ||
                            lower.contains("natal") || lower.contains("christmas") ||
                            lower.contains("idul fitri") || lower.contains("idul adha") ||
                            lower.contains("tahun baru") || lower.contains("new year") ||
                            lower.contains("buruh") || lower.contains("pancasila") ||
                            lower.contains("islamic new year") || lower.contains("maulid")

                    tvDetailStatusBadge.text = if (isPublicHoliday) {
                        getString(R.string.calendar_status_public_holiday)
                    } else {
                        getString(R.string.calendar_status_observance)
                    }
                    tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
                    tvDetailObservanceDesc.text = "$holidayName — $holidayDesc"
                    tvDetailObservanceDesc.visibility = View.VISIBLE
                } else {
                    tvDetailStatusBadge.text = getString(R.string.calendar_status_regular_day)
                    tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    tvDetailObservanceDesc.text = getString(R.string.calendar_status_no_record)
                    tvDetailObservanceDesc.visibility = View.VISIBLE
                }
            } else {
                layoutActiveDetail.visibility = View.GONE
                tvNeutralPrompt.visibility = View.VISIBLE
            }
        }

        calendarViewModel.initCalendar(this)
    }

    // ============================
    // MARKET WIDGET
    // ============================
    private fun setupMarketSection() {
        tvMarketTitle.setOnClickListener {
            cryptoViewModel.loadMarketData(this, forceRefresh = true)
        }

        cryptoViewModel.uiState.observe(this) { state ->
            when (state) {
                is CryptoUiState.Loading -> {
                    // Retain current view state during loading
                }
                is CryptoUiState.Success -> {
                    displayCryptoRows(state.assets)

                    if (state.isDemo) {
                        tvMarketStatusIndicator.text = getString(R.string.market_status_demo)
                        tvMarketStatusIndicator.visibility = View.VISIBLE
                    } else if (state.isCached) {
                        tvMarketStatusIndicator.text = getString(R.string.market_status_offline)
                        tvMarketStatusIndicator.visibility = View.VISIBLE
                    } else {
                        tvMarketStatusIndicator.visibility = View.GONE
                    }
                }
                is CryptoUiState.Error -> {
                    tvMarketStatusIndicator.text = state.message
                    tvMarketStatusIndicator.visibility = View.VISIBLE
                }
            }
        }

        cryptoViewModel.loadMarketData(this)
    }

    private fun displayCryptoRows(assets: List<CryptoAsset>) {
        layoutCryptoRows.removeAllViews()
        val inflater = LayoutInflater.from(this)

        assets.forEachIndexed { index, asset ->
            val rowView = inflater.inflate(R.layout.item_crypto_row, layoutCryptoRows, false)

            val ivCoinLogo = rowView.findViewById<ImageView>(R.id.ivCoinLogo)
            val tvLogoFallback = rowView.findViewById<TextView>(R.id.tvLogoFallback)
            val tvCoinName = rowView.findViewById<TextView>(R.id.tvCoinName)
            val tvCoinSymbol = rowView.findViewById<TextView>(R.id.tvCoinSymbol)
            val sparklineView = rowView.findViewById<SparklineView>(R.id.sparklineView)
            val tvCoinPrice = rowView.findViewById<TextView>(R.id.tvCoinPrice)
            val tvCoinChange = rowView.findViewById<TextView>(R.id.tvCoinChange)

            tvCoinName.text = asset.name
            tvCoinSymbol.text = asset.symbol

            // Logo
            if (!asset.imageUrl.isNullOrEmpty()) {
                tvLogoFallback.visibility = View.GONE
                ivCoinLogo.visibility = View.VISIBLE
                ivCoinLogo.load(asset.imageUrl) {
                    crossfade(true)
                    listener(
                        onError = { _, _ ->
                            ivCoinLogo.visibility = View.GONE
                            tvLogoFallback.visibility = View.VISIBLE
                            tvLogoFallback.text = asset.symbol.take(1)
                        }
                    )
                }
            } else {
                ivCoinLogo.visibility = View.GONE
                tvLogoFallback.visibility = View.VISIBLE
                tvLogoFallback.text = asset.symbol.take(1)
            }

            // Price
            if (asset.currentPriceUsd != null) {
                val formattedPrice = if (asset.currentPriceUsd >= 1.0) {
                    String.format(Locale.US, "$%,.2f", asset.currentPriceUsd)
                } else {
                    String.format(Locale.US, "$%,.4f", asset.currentPriceUsd)
                }
                tvCoinPrice.text = formattedPrice
                tvCoinPrice.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            } else {
                tvCoinPrice.text = "--"
                tvCoinPrice.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            }

            // 24h Change
            if (asset.priceChange24h != null) {
                val isPositive = asset.priceChange24h >= 0
                val changeText = if (isPositive) {
                    String.format(Locale.US, "+%.2f%%", asset.priceChange24h)
                } else {
                    String.format(Locale.US, "%.2f%%", asset.priceChange24h)
                }
                tvCoinChange.text = changeText
                val colorRes = if (isPositive) R.color.accent_teal else R.color.accent_muted_red
                tvCoinChange.setTextColor(ContextCompat.getColor(this, colorRes))
                tvCoinChange.visibility = View.VISIBLE
            } else {
                tvCoinChange.text = "--"
                tvCoinChange.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            }

            // Sparkline
            if (!asset.sparklinePrices.isNullOrEmpty() && asset.sparklinePrices.size >= 2) {
                val isPositive = (asset.priceChange24h ?: 0.0) >= 0
                sparklineView.setSparklineData(asset.sparklinePrices, isPositive)
                sparklineView.visibility = View.VISIBLE
            } else {
                sparklineView.setSparklineData(null, true)
                sparklineView.visibility = View.INVISIBLE
            }

            layoutCryptoRows.addView(rowView)

            // Divider except after the last item
            if (index < assets.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * resources.displayMetrics.density).toInt()
                    )
                    setBackgroundColor(Color.parseColor("#1AFFFFFF"))
                }
                layoutCryptoRows.addView(divider)
            }
        }
    }

    // ============================
    // NEWS WIDGET
    // ============================
    private fun setupNewsSection() {
        layoutNewsHeaderTitle.setOnClickListener {
            val intent = Intent(this, com.repp.aquaticremineztools.presentation.news.NewsActivity::class.java)
            ActivityTransition.startActivitySmooth(this, intent)
        }

        newsViewModel.homepageUiState.observe(this) { state ->
            when (state) {
                is NewsUiState.Loading -> {
                    if (containerFeaturedArticle.childCount == 0 && layoutCompactArticles.childCount == 0) {
                        pbNewsLoading.visibility = View.VISIBLE
                    }
                }
                is NewsUiState.Success -> {
                    pbNewsLoading.visibility = View.GONE
                    displayHomepageNews(state.articles)

                    if (state.isOffline || state.isCached) {
                        tvNewsStatusIndicator.text = getString(R.string.news_offline_notice)
                        tvNewsStatusIndicator.visibility = View.VISIBLE
                    } else {
                        tvNewsStatusIndicator.visibility = View.GONE
                    }
                }
                is NewsUiState.Error -> {
                    pbNewsLoading.visibility = View.GONE
                    tvNewsStatusIndicator.text = state.message
                    tvNewsStatusIndicator.visibility = View.VISIBLE
                }
            }
        }

        newsViewModel.loadHomepageNews(this)
    }

    private fun displayHomepageNews(articles: List<NewsArticle>) {
        containerFeaturedArticle.removeAllViews()
        layoutCompactArticles.removeAllViews()

        if (articles.isEmpty()) return

        val inflater = LayoutInflater.from(this)

        // 1. Featured Article
        val featuredArticle = articles.firstOrNull { it.isFeatured } ?: articles.first()
        val featuredView = inflater.inflate(R.layout.item_news_featured, containerFeaturedArticle, false)

        val ivFeaturedThumbnail = featuredView.findViewById<ImageView>(R.id.ivFeaturedThumbnail)
        val tvFeaturedCategory = featuredView.findViewById<TextView>(R.id.tvFeaturedCategory)
        val tvFeaturedHeadline = featuredView.findViewById<TextView>(R.id.tvFeaturedHeadline)
        val tvFeaturedMeta = featuredView.findViewById<TextView>(R.id.tvFeaturedMeta)
        val tvFeaturedSummary = featuredView.findViewById<TextView>(R.id.tvFeaturedSummary)

        tvFeaturedCategory.text = featuredArticle.category.uppercase(Locale.ROOT)
        tvFeaturedHeadline.text = featuredArticle.headline
        val relativeTime = DateUtils.getRelativeTimeSpanString(
            featuredArticle.publishedAtEpoch,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        tvFeaturedMeta.text = "${featuredArticle.publisher} · $relativeTime"
        tvFeaturedSummary.text = featuredArticle.summary

        if (!featuredArticle.thumbnailUrl.isNullOrBlank()) {
            ivFeaturedThumbnail.load(featuredArticle.thumbnailUrl) {
                placeholder(R.drawable.home_background_image)
                error(R.drawable.home_background_image)
                crossfade(true)
            }
        } else {
            ivFeaturedThumbnail.setImageResource(R.drawable.home_background_image)
        }

        featuredView.setOnClickListener {
            openNewsDetail(featuredArticle)
        }

        containerFeaturedArticle.addView(featuredView)

        // 2. Compact Articles
        val compactList = articles.filter { it.articleUrl != featuredArticle.articleUrl }.take(7)
        compactList.forEach { article ->
            val compactView = inflater.inflate(R.layout.item_news_compact, layoutCompactArticles, false)

            val ivCompactThumbnail = compactView.findViewById<ImageView>(R.id.ivCompactThumbnail)
            val tvCompactCategory = compactView.findViewById<TextView>(R.id.tvCompactCategory)
            val tvCompactMeta = compactView.findViewById<TextView>(R.id.tvCompactMeta)
            val tvCompactHeadline = compactView.findViewById<TextView>(R.id.tvCompactHeadline)
            val tvCompactSummary = compactView.findViewById<TextView>(R.id.tvCompactSummary)

            tvCompactCategory.text = article.category.uppercase(Locale.ROOT)
            tvCompactHeadline.text = article.headline
            tvCompactSummary.text = article.summary

            val compRelativeTime = DateUtils.getRelativeTimeSpanString(
                article.publishedAtEpoch,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            tvCompactMeta.text = "${article.publisher} · $compRelativeTime"

            if (!article.thumbnailUrl.isNullOrBlank()) {
                ivCompactThumbnail.load(article.thumbnailUrl) {
                    placeholder(R.drawable.home_background_image)
                    error(R.drawable.home_background_image)
                    crossfade(true)
                }
            } else {
                ivCompactThumbnail.setImageResource(R.drawable.home_background_image)
            }

            compactView.setOnClickListener {
                openNewsDetail(article)
            }

            layoutCompactArticles.addView(compactView)
        }
    }

    private fun openNewsDetail(article: NewsArticle) {
        val intent = Intent(this, com.repp.aquaticremineztools.presentation.news.NewsDetailActivity::class.java).apply {
            putExtra("article_url", article.articleUrl)
            putExtra("headline", article.headline)
            putExtra("summary", article.summary)
            putExtra("content", article.content)
            putExtra("thumbnail_url", article.thumbnailUrl)
            putExtra("publisher", article.publisher)
            putExtra("category", article.category)
            putExtra("published_epoch", article.publishedAtEpoch)
        }
        ActivityTransition.startActivitySmooth(this, intent)
    }

    // ============================
    // QUICK MENU
    // ============================
    private fun setupQuickMenu() {
        val rvQuickMenu: RecyclerView = findViewById(R.id.rvQuickMenu)
        val categories = QuickCategory.getAll()
        val frameClearDrawable = window.decorView.background

        val adapter = QuickCategoryAdapter(categories, homeBlurTarget, frameClearDrawable) { category ->
            Toast.makeText(
                this,
                "${getString(category.nameRes)}\nComing soon!",
                Toast.LENGTH_SHORT,
            ).show()
        }

        rvQuickMenu.layoutManager = GridLayoutManager(this, 4)
        rvQuickMenu.adapter = adapter
    }

    // ============================
    // INFO CARDS (IP, Connection, Ping)
    // ============================
    private fun setupInfoCards() {
        updateIpAndConnection()
        pingHandler.post(pingRunnable)
    }

    private fun updateIpAndConnection() {
        executor.execute {
            val ip = InfoCardHelper.getLocalIpAddress()
            val conn = InfoCardHelper.getConnectionInfo(this)

            runOnUiThread {
                tvIpValue.text = if (ip.isEmpty() || ip == "Unavailable") {
                    getString(R.string.info_unavailable)
                } else {
                    ip
                }

                // Update label + value
                tvConnectionLabel.text = conn.type

                tvConnectionValue.text = if (!conn.isConnected) {
                    getString(R.string.info_not_connected)
                } else if (conn.detail.isEmpty()) {
                    "Connected"
                } else {
                    conn.detail
                }

                val ivConnectionIcon = findViewById<ImageView>(R.id.ivConnectionIcon)
                if (conn.type == "WI-FI") {
                    ivConnectionIcon?.setImageResource(R.drawable.ic_wifi)
                    ivConnectionIcon?.imageTintList = ContextCompat.getColorStateList(this, R.color.accent_teal)
                } else if (conn.type == "DATA SELULER") {
                    ivConnectionIcon?.setImageResource(R.drawable.ic_network_cat)
                    ivConnectionIcon?.imageTintList = ContextCompat.getColorStateList(this, R.color.accent_cyan)
                } else {
                    ivConnectionIcon?.setImageResource(R.drawable.ic_network)
                    ivConnectionIcon?.imageTintList = ContextCompat.getColorStateList(this, R.color.text_secondary)
                }
            }
        }
    }

    private fun updatePingValue() {
        executor.execute {
            val ping = InfoCardHelper.pingHost("1.1.1.1", 3000)
            runOnUiThread {
                tvPingValue.text = if (ping >= 0) "$ping ms" else getString(R.string.info_timeout)
            }
        }
    }

    // ============================
    // VIDEO
    // ============================
    private fun setupVideo() {
        val player = ExoPlayer.Builder(this).build()
        val uri = "android.resource://$packageName/${R.raw.home_card_video}".toUri()

        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.volume = 1.0f
        player.playWhenReady = true
        player.prepare()

        videoUserCard.player = player
        cardPlayer = player
    }

    private fun setupMuteButton() {
        btnMuteCard.setOnClickListener {
            isCardMuted = !isCardMuted
            cardPlayer?.volume = if (isCardMuted) 0f else 1.0f
            ivMuteCard.setImageResource(
                if (isCardMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on,
            )
        }
    }

    // ============================
    // CLOCK + DATE
    // ============================
    private fun startClock() {
        clockHandler.post(clockRunnable)
        updateClock()
        updateTodayDate()
        tvCreatedDate.text = "Created: 11-10-2026"
        refreshBattery()
    }

    private fun updateClock() {
        tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun updateTodayDate() {
        val locale = Locale.US
        val fmt = SimpleDateFormat("EEEE, MMMM d, yyyy", locale)
        tvTodayDate.text = fmt.format(Date())
    }

    // ============================
    // BATTERY
    // ============================
    private fun registerBattery() {
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun refreshBattery() {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level != -1 && scale != -1) {
            updateBatteryUI((level * 100 / scale.toFloat()).toInt())
        }
    }

    private fun updateBatteryUI(pct: Int) {
        tvBattery.text = "$pct%"

        val isCharging = {
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }()

        val iconRes: Int
        val tintRes: Int

        when {
            isCharging -> {
                iconRes = R.drawable.ic_battery
                tintRes = R.color.status_online
            }
            pct >= 60 -> {
                iconRes = R.drawable.ic_battery_full
                tintRes = R.color.status_online
            }
            pct >= 20 -> {
                iconRes = R.drawable.ic_battery_half
                tintRes = R.color.status_warning
            }
            else -> {
                iconRes = R.drawable.ic_battery_low
                tintRes = R.color.status_error
            }
        }

        ivBatteryIcon.setImageResource(iconRes)
        ivBatteryIcon.setColorFilter(getColor(tintRes))
    }

    // ============================
    // LOCATION + WEATHER
    // ============================
    private fun requestLocationAndWeather() {
        lastLocationFetch = System.currentTimeMillis()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            getLocationAndWeather()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                LOCATION_PERMISSION_CODE,
            )
            fetchLocationFromIp()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndWeather()
            } else {
                fetchLocationFromIp()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndWeather() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )

            var lastLocation: Location? = null
            for (provider in providers) {
                try {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (lastLocation == null || loc.time > lastLocation.time) {
                            lastLocation = loc
                        }
                    }
                } catch (_: Exception) { }
            }

            if (lastLocation != null) {
                val lat = lastLocation.latitude
                val lon = lastLocation.longitude

                executor.execute {
                    val cityName = getCityName(lat, lon)
                    if (cityName != "Unknown" && cityName.isNotEmpty()) {
                        runOnUiThread {
                            tvLocation.text = cityName
                            prayerViewModel.loadSchedule(this@HomeActivity, lat, lon, cityName)
                        }
                    } else {
                        fetchLocationFromIp()
                    }
                }
                fetchWeather(lat, lon)
            } else {
                fetchLocationFromIp()

                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val lat = location.latitude
                        val lon = location.longitude

                        executor.execute {
                            val cityName = getCityName(lat, lon)
                            if (cityName != "Unknown" && cityName.isNotEmpty()) {
                                runOnUiThread {
                                    tvLocation.text = cityName
                                    prayerViewModel.loadSchedule(this@HomeActivity, lat, lon, cityName)
                                }
                            }
                        }
                        fetchWeather(lat, lon)
                        try { lm.removeUpdates(this) } catch (_: Exception) {}
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                    } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("WEATHER_DEBUG", "Error lokasi: ${e.message}", e)
            fetchLocationFromIp()
        }
    }

    private fun fetchLocationFromIp() {
        executor.execute {
            try {
                val url = URL("http://ip-api.com/json/")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(response)
                if (json.optString("status") == "success") {
                    val city = json.optString("city", "")
                    val lat = json.optDouble("lat", 0.0)
                    val lon = json.optDouble("lon", 0.0)

                    val cleanedCity = cleanCityName(city)
                    if (cleanedCity.isNotEmpty() && cleanedCity != "Unknown") {
                        runOnUiThread {
                            tvLocation.text = cleanedCity
                            if (lat != 0.0 && lon != 0.0) {
                                prayerViewModel.loadSchedule(this@HomeActivity, lat, lon, cleanedCity)
                            }
                        }
                    } else {
                        if (lat != 0.0 && lon != 0.0) {
                            prayerViewModel.loadSchedule(this@HomeActivity, lat, lon, "Jakarta")
                        } else {
                            prayerViewModel.loadSchedule(this@HomeActivity)
                        }
                    }
                    if (lat != 0.0 && lon != 0.0) {
                        fetchWeather(lat, lon)
                    }
                } else {
                    prayerViewModel.loadSchedule(this@HomeActivity)
                }
            } catch (e: Exception) {
                Log.e("WEATHER_DEBUG", "IP location error: ${e.message}", e)
                prayerViewModel.loadSchedule(this@HomeActivity)
            }
        }
    }

    private fun getCityName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)

            if (addresses.isNullOrEmpty()) return "Unknown"

            val addr = addresses[0]
            val rawName = addr.locality
                ?: addr.subAdminArea
                ?: addr.adminArea
                ?: "Unknown"

            cleanCityName(rawName)
        } catch (e: Exception) {
            Log.e("WEATHER_DEBUG", "Geocoder error: ${e.message}", e)
            "Unknown"
        }
    }

    private fun cleanCityName(raw: String): String {
        val beforeComma = raw.substringBefore(",").trim()
        return beforeComma
            .replace("Kecamatan ", "", ignoreCase = true)
            .replace("Kabupaten ", "", ignoreCase = true)
            .replace("Kota ", "", ignoreCase = true)
            .replace("Desa ", "", ignoreCase = true)
            .replace("Kelurahan ", "", ignoreCase = true)
            .replace("Provinsi ", "", ignoreCase = true)
            .replace("Daerah Istimewa ", "", ignoreCase = true)
            .replace("DKI ", "", ignoreCase = true)
            .trim()
            .ifEmpty { raw.trim() }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        executor.execute {
            try {
                val urlStr = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current_weather=true"

                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(response)
                val currentWeather = json.getJSONObject("current_weather")
                val temp = currentWeather.getDouble("temperature")
                val code = currentWeather.getInt("weathercode")

                val emoji = weatherCodeToEmoji(code)
                val tempRound = temp.roundToInt()

                runOnUiThread {
                    tvWeatherIcon.text = emoji
                    tvTemperature.text = "$tempRound°C"
                }
            } catch (e: Exception) {
                Log.e("WEATHER_DEBUG", "Weather error: ${e.message}", e)
                runOnUiThread {
                    tvWeatherIcon.text = "❓"
                    tvTemperature.text = "--°C"
                }
            }
        }
    }

    private fun weatherCodeToEmoji(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2, 3 -> "🌤️"
            45, 48 -> "🌫️"
            51, 53, 55 -> "🌦️"
            56, 57 -> "🌧️"
            61, 63, 65 -> "🌧️"
            66, 67 -> "🌧️"
            71, 73, 75 -> "❄️"
            77 -> "🌨️"
            80, 81, 82 -> "🌧️"
            85, 86 -> "🌨️"
            95 -> "⛈️"
            96, 99 -> "⛈️"
            else -> "🌤️"
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectNavTab(0)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        cardPlayer?.play()
        updatePasswordLabel()
        selectNavTab(0)
        prayerCountdownHandler.post(prayerCountdownRunnable)

        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        updateIpAndConnection()

        val now = System.currentTimeMillis()
        if (now - lastLocationFetch > LOCATION_REFRESH_INTERVAL) {
            requestLocationAndWeather()
        }
    }

    override fun onPause() {
        super.onPause()
        cardPlayer?.pause()
        prayerCountdownHandler.removeCallbacks(prayerCountdownRunnable)
        AppLockManager.onActivityPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        pingHandler.removeCallbacks(pingRunnable)
        prayerCountdownHandler.removeCallbacks(prayerCountdownRunnable)
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        cardPlayer?.release()
        cardPlayer = null
        executor.shutdown()
    }
}