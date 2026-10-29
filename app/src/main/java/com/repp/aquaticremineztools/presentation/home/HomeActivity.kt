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
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.network.InfoCardHelper
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.core.settings.PrayerPreferences
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.core.util.DrawerMenuHelper
import com.repp.aquaticremineztools.core.util.HijriCalendarHelper
import com.repp.aquaticremineztools.data.QuickCategory
import com.repp.aquaticremineztools.data.model.CryptoAsset
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.NewsArticle
import com.repp.aquaticremineztools.presentation.market.CryptoDashboardAdapter
import com.repp.aquaticremineztools.presentation.market.EconomicEventAdapter
import com.repp.aquaticremineztools.presentation.market.HeroInstrumentSpinnerAdapter
import com.repp.aquaticremineztools.presentation.market.EconomicIndicatorAdapter
import com.repp.aquaticremineztools.presentation.market.ForexMatrixAdapter
import com.repp.aquaticremineztools.presentation.market.KeyMarketsAdapter
import com.repp.aquaticremineztools.presentation.market.MarketChartMarkerView
import com.repp.aquaticremineztools.presentation.market.MarketInstrumentBottomSheet
import com.repp.aquaticremineztools.presentation.market.MarketMoversAdapter
import com.repp.aquaticremineztools.presentation.market.MarketPulseAdapter
import com.repp.aquaticremineztools.presentation.market.MarketUiState
import com.repp.aquaticremineztools.presentation.market.MarketViewModel
import com.repp.aquaticremineztools.presentation.market.StockWatchlistAdapter
import com.repp.aquaticremineztools.presentation.news.NewsUiState
import com.repp.aquaticremineztools.presentation.news.NewsViewModel
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.security.PinChangeActivity
import com.repp.aquaticremineztools.presentation.security.PinSetupActivity
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.about.AboutActivity
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

    // Market Hub Widget
    private lateinit var marketViewModel: MarketViewModel
    private lateinit var cardMarketBlur: BlurView
    private lateinit var tvMarketTitle: TextView
    private lateinit var tvMarketSessionStatus: TextView
    private lateinit var tvMarketConditionSummary: TextView
    private lateinit var btnMarketRefresh: FrameLayout
    private lateinit var tvMarketStatusIndicator: TextView

    // Section 1: Pulse
    private lateinit var rvMarketPulse: RecyclerView
    private lateinit var pulseAdapter: MarketPulseAdapter

    // Section 2: Hero Chart
    private lateinit var chipHeroCrypto: TextView
    private lateinit var chipHeroStocks: TextView
    private lateinit var chipHeroIndices: TextView
    private lateinit var chipHeroForex: TextView
    private lateinit var spinnerHeroBenchmark: Spinner
    private var heroSpinnerAdapter: HeroInstrumentSpinnerAdapter? = null
    private lateinit var tvHeroSymbol: TextView
    private lateinit var tvHeroPrice: TextView
    private lateinit var tvHeroChange: TextView
    private lateinit var tvHeroDataStatus: TextView
    private lateinit var btnIndicatorMa: TextView
    private lateinit var btnIndicatorEma: TextView
    private lateinit var btnIndicatorRsi: TextView
    private lateinit var btnIndicatorMacd: TextView
    private lateinit var tvIndicatorStatus: TextView
    private lateinit var chartHero: com.github.mikephil.charting.charts.LineChart
    private lateinit var btnHero1D: TextView
    private lateinit var btnHero1W: TextView
    private lateinit var btnHero1M: TextView
    private lateinit var btnHero3M: TextView
    private lateinit var btnHero1Y: TextView
    private lateinit var btnHeroALL: TextView
    private lateinit var btnHeroCustomDate: TextView

    // Section 3: Key Markets
    private lateinit var rvKeyMarkets: RecyclerView
    private lateinit var keyMarketsAdapter: KeyMarketsAdapter

    // Section 4: Top Gainers & Losers
    private lateinit var rvTopGainers: RecyclerView
    private lateinit var topGainersAdapter: MarketMoversAdapter
    private lateinit var rvTopLosers: RecyclerView
    private lateinit var topLosersAdapter: MarketMoversAdapter

    // Section 5: Crypto
    private lateinit var rvCryptoMarket: RecyclerView
    private lateinit var cryptoDashboardAdapter: CryptoDashboardAdapter

    // Section 6: Stocks & ETFs
    private lateinit var rvStocksAndEtfs: RecyclerView
    private lateinit var stockAdapter: StockWatchlistAdapter

    // Section 7: Forex Matrix
    private lateinit var rvForexMatrix: RecyclerView
    private lateinit var forexAdapter: ForexMatrixAdapter

    // Section 8: Economic Indicators
    private lateinit var rvEconomicIndicators: RecyclerView
    private lateinit var economicIndicatorAdapter: EconomicIndicatorAdapter

    // Section 9: Economic Events
    private lateinit var rvEconomicEvents: RecyclerView
    private lateinit var economicEventAdapter: EconomicEventAdapter

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
        marketViewModel = ViewModelProvider(this)[MarketViewModel::class.java]
        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        newsViewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        prayerViewModel.loadSchedule(this)
        prayerCountdownHandler.post(prayerCountdownRunnable)

        bindViews()
        setupSwipeRefresh()
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

    private fun setupSwipeRefresh() {
        val swipeRefreshLayout = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#1E222B"))
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#00E5FF"), Color.parseColor("#10B981"))
        swipeRefreshLayout.setOnRefreshListener {
            marketViewModel.loadMarket(this, true)
            newsViewModel.loadHomepageNews(this, true)
            updateIpAndConnection()
            updatePingValue()
            Handler(Looper.getMainLooper()).postDelayed({
                swipeRefreshLayout.isRefreshing = false
            }, 1200L)
        }
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

        // Market Hub Views
        cardMarketBlur = findViewById(R.id.cardMarket)
        tvMarketTitle = findViewById(R.id.tvMarketTitle)
        tvMarketSessionStatus = findViewById(R.id.tvMarketSessionStatus)
        tvMarketConditionSummary = findViewById(R.id.tvMarketConditionSummary)
        btnMarketRefresh = findViewById(R.id.btnMarketRefresh)
        tvMarketStatusIndicator = findViewById(R.id.tvMarketStatusIndicator)

        rvMarketPulse = findViewById(R.id.rvMarketPulse)
        chipHeroCrypto = findViewById(R.id.chipHeroCrypto)
        chipHeroStocks = findViewById(R.id.chipHeroStocks)
        chipHeroIndices = findViewById(R.id.chipHeroIndices)
        chipHeroForex = findViewById(R.id.chipHeroForex)
        spinnerHeroBenchmark = findViewById(R.id.spinnerHeroBenchmark)
        tvHeroSymbol = findViewById(R.id.tvHeroSymbol)
        tvHeroPrice = findViewById(R.id.tvHeroPrice)
        tvHeroChange = findViewById(R.id.tvHeroChange)
        tvHeroDataStatus = findViewById(R.id.tvHeroDataStatus)
        btnIndicatorMa = findViewById(R.id.btnIndicatorMa)
        btnIndicatorEma = findViewById(R.id.btnIndicatorEma)
        btnIndicatorRsi = findViewById(R.id.btnIndicatorRsi)
        btnIndicatorMacd = findViewById(R.id.btnIndicatorMacd)
        tvIndicatorStatus = findViewById(R.id.tvIndicatorStatus)
        chartHero = findViewById(R.id.chartHero)
        btnHero1D = findViewById(R.id.btnHero1D)
        btnHero1W = findViewById(R.id.btnHero1W)
        btnHero1M = findViewById(R.id.btnHero1M)
        btnHero3M = findViewById(R.id.btnHero3M)
        btnHero1Y = findViewById(R.id.btnHero1Y)
        btnHeroALL = findViewById(R.id.btnHeroALL)
        btnHeroCustomDate = findViewById(R.id.btnHeroCustomDate)

        rvKeyMarkets = findViewById(R.id.rvKeyMarkets)
        rvTopGainers = findViewById(R.id.rvTopGainers)
        rvTopLosers = findViewById(R.id.rvTopLosers)
        rvCryptoMarket = findViewById(R.id.rvCryptoMarket)
        rvStocksAndEtfs = findViewById(R.id.rvStocksAndEtfs)
        rvForexMatrix = findViewById(R.id.rvForexMatrix)
        rvEconomicIndicators = findViewById(R.id.rvEconomicIndicators)
        rvEconomicEvents = findViewById(R.id.rvEconomicEvents)

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

    private fun updatePasswordLabel() {
        if (::tvPasswordLabel.isInitialized) {
            tvPasswordLabel.text = if (PinManager.hasPin(this)) {
                getString(R.string.drawer_change_password)
            } else {
                getString(R.string.drawer_create_password)
            }
        }
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
        findViewById<View>(R.id.btnPrayerRefresh)?.setOnClickListener {
            prayerViewModel.loadSchedule(this)
            Toast.makeText(this, "Refreshing Prayer Schedule...", Toast.LENGTH_SHORT).show()
        }

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
        findViewById<View>(R.id.btnCalendarRefresh)?.setOnClickListener {
            calendarViewModel.initCalendar(this)
            Toast.makeText(this, "Refreshing Calendar...", Toast.LENGTH_SHORT).show()
        }

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
    // MARKET HUB WIDGET (ALL 9 SECTIONS)
    // ============================
    private fun setupMarketSection() {
        btnMarketRefresh.setOnClickListener {
            marketViewModel.loadMarket(this, forceRefresh = true)
        }

        pulseAdapter = MarketPulseAdapter(emptyList(), ::onInstrumentClicked)
        keyMarketsAdapter = KeyMarketsAdapter(emptyList(), ::onInstrumentClicked)
        topGainersAdapter = MarketMoversAdapter(emptyList(), ::onInstrumentClicked)
        topLosersAdapter = MarketMoversAdapter(emptyList(), ::onInstrumentClicked)
        cryptoDashboardAdapter = CryptoDashboardAdapter(emptyList(), ::onInstrumentClicked)
        stockAdapter = StockWatchlistAdapter(emptyList(), ::onInstrumentClicked)
        forexAdapter = ForexMatrixAdapter(emptyList(), ::onInstrumentClicked)
        economicIndicatorAdapter = EconomicIndicatorAdapter(emptyList(), ::onInstrumentClicked)
        economicEventAdapter = EconomicEventAdapter(emptyList())

        rvMarketPulse.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMarketPulse.adapter = pulseAdapter
        rvMarketPulse.isNestedScrollingEnabled = false

        rvKeyMarkets.layoutManager = GridLayoutManager(this, 2)
        rvKeyMarkets.adapter = keyMarketsAdapter
        rvKeyMarkets.isNestedScrollingEnabled = false

        rvTopGainers.layoutManager = LinearLayoutManager(this)
        rvTopGainers.adapter = topGainersAdapter
        rvTopGainers.isNestedScrollingEnabled = false

        rvTopLosers.layoutManager = LinearLayoutManager(this)
        rvTopLosers.adapter = topLosersAdapter
        rvTopLosers.isNestedScrollingEnabled = false

        rvCryptoMarket.layoutManager = LinearLayoutManager(this)
        rvCryptoMarket.adapter = cryptoDashboardAdapter
        rvCryptoMarket.isNestedScrollingEnabled = false

        rvStocksAndEtfs.layoutManager = LinearLayoutManager(this)
        rvStocksAndEtfs.adapter = stockAdapter
        rvStocksAndEtfs.isNestedScrollingEnabled = false

        rvForexMatrix.layoutManager = GridLayoutManager(this, 2)
        rvForexMatrix.adapter = forexAdapter
        rvForexMatrix.isNestedScrollingEnabled = false

        rvEconomicIndicators.layoutManager = GridLayoutManager(this, 2)
        rvEconomicIndicators.adapter = economicIndicatorAdapter
        rvEconomicIndicators.isNestedScrollingEnabled = false

        rvEconomicEvents.layoutManager = LinearLayoutManager(this)
        rvEconomicEvents.adapter = economicEventAdapter
        rvEconomicEvents.isNestedScrollingEnabled = false

        // Category Chips for Hero Chart
        chipHeroCrypto.setOnClickListener {
            marketViewModel.setHeroCategory(MarketCategory.CRYPTO, "bitcoin")
        }
        chipHeroStocks.setOnClickListener {
            marketViewModel.setHeroCategory(MarketCategory.STOCKS, "bbca")
        }
        chipHeroIndices.setOnClickListener {
            marketViewModel.setHeroCategory(MarketCategory.INDICES, "ihsg")
        }
        chipHeroForex.setOnClickListener {
            marketViewModel.setHeroCategory(MarketCategory.FOREX, "usd_idr")
        }

        // Time Range Buttons
        val rangeButtons = listOf(
            Pair(btnHero1D, "1D"),
            Pair(btnHero1W, "1W"),
            Pair(btnHero1M, "1M"),
            Pair(btnHero3M, "3M"),
            Pair(btnHero1Y, "1Y"),
            Pair(btnHeroALL, "ALL")
        )
        rangeButtons.forEach { (btn, range) ->
            btn.setOnClickListener { marketViewModel.setHeroTimeRange(range) }
        }

        btnHeroCustomDate.setOnClickListener {
            showCustomDatePicker()
        }

        // Technical Indicator Toggles
        btnIndicatorMa.setOnClickListener { marketViewModel.toggleIndicator("MA") }
        btnIndicatorEma.setOnClickListener { marketViewModel.toggleIndicator("EMA") }
        btnIndicatorRsi.setOnClickListener { marketViewModel.toggleIndicator("RSI") }
        btnIndicatorMacd.setOnClickListener { marketViewModel.toggleIndicator("MACD") }

        // Observe ViewModel State
        marketViewModel.uiState.observe(this) { state ->
            when (state) {
                is MarketUiState.Loading -> {
                    tvMarketStatusIndicator.text = "Syncing Market Feeds..."
                    tvMarketStatusIndicator.visibility = View.VISIBLE
                }
                is MarketUiState.Success -> {
                    tvMarketStatusIndicator.visibility = View.GONE

                    tvMarketSessionStatus.text = state.sessionStatus
                    tvMarketConditionSummary.text = "${state.marketConditionSummary} · ${state.lastUpdatedFormatted}"

                    pulseAdapter.updateData(state.marketPulse)
                    keyMarketsAdapter.updateData(state.keyMarkets)
                    topGainersAdapter.updateData(state.topGainers)
                    topLosersAdapter.updateData(state.topLosers)
                    cryptoDashboardAdapter.updateData(state.cryptoInstruments)
                    stockAdapter.updateData(state.stocksInstruments)
                    forexAdapter.updateData(state.forexInstruments)
                    economicIndicatorAdapter.updateData(state.economicIndicators)
                    economicEventAdapter.updateData(state.economicEvents)

                    updateCategoryChipsStyle(state.selectedHeroCategory)

                    val filteredCategoryInstruments = state.allInstruments.filter {
                        if (state.selectedHeroCategory == MarketCategory.STOCKS) {
                            it.category == MarketCategory.STOCKS || it.category == MarketCategory.ETFS
                        } else {
                            it.category == state.selectedHeroCategory
                        }
                    }

                    if (filteredCategoryInstruments.isNotEmpty()) {
                        if (heroSpinnerAdapter == null) {
                            heroSpinnerAdapter = HeroInstrumentSpinnerAdapter(this, filteredCategoryInstruments)
                            spinnerHeroBenchmark.adapter = heroSpinnerAdapter
                        }
                        heroSpinnerAdapter?.updateData(filteredCategoryInstruments, state.selectedHeroBenchmarkId)

                        var selectedIdx = filteredCategoryInstruments.indexOfFirst { it.id == state.selectedHeroBenchmarkId }
                        if (selectedIdx == -1) selectedIdx = 0

                        spinnerHeroBenchmark.onItemSelectedListener = null
                        spinnerHeroBenchmark.setSelection(selectedIdx, false)

                        spinnerHeroBenchmark.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                val selectedInst = filteredCategoryInstruments.getOrNull(position)
                                if (selectedInst != null && selectedInst.id != state.selectedHeroBenchmarkId) {
                                    marketViewModel.setHeroBenchmark(selectedInst.id)
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }

                        val activeHeroInst = filteredCategoryInstruments.find { it.id == state.selectedHeroBenchmarkId } 
                            ?: filteredCategoryInstruments.firstOrNull() 
                            ?: return@observe
                        renderHeroChart(
                            activeHeroInst,
                            state.selectedHeroTimeRange,
                            state.heroStartDateMs,
                            state.heroEndDateMs,
                            state.isMaEnabled,
                            state.isEmaEnabled,
                            state.isRsiEnabled,
                            state.isMacdEnabled
                        )
                    }

                    updateTimeRangeButtonsStyle(state.selectedHeroTimeRange)
                    updateIndicatorButtonsStyle(state.isMaEnabled, state.isEmaEnabled, state.isRsiEnabled, state.isMacdEnabled)
                }
                is MarketUiState.Error -> {
                    tvMarketStatusIndicator.text = state.message
                    tvMarketStatusIndicator.visibility = View.VISIBLE
                }
            }
        }

        marketViewModel.loadMarket(this)
    }

    private fun onInstrumentClicked(instrument: MarketInstrument) {
        val bottomSheet = MarketInstrumentBottomSheet.newInstance(instrument)
        bottomSheet.show(supportFragmentManager, "MarketInstrumentBottomSheet")
    }

    private fun showCustomDatePicker() {
        val builder = com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select Hero Chart Date Range")
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val startMs = selection.first
            val endMs = selection.second
            if (startMs != null && endMs != null) {
                marketViewModel.setHeroTimeRange("CUSTOM", startMs, endMs)
            }
        }
        picker.show(supportFragmentManager, "hero_date_range_picker")
    }

    private fun updateCategoryChipsStyle(activeCategory: MarketCategory) {
        val chipMap = listOf(
            Pair(chipHeroCrypto, MarketCategory.CRYPTO),
            Pair(chipHeroStocks, MarketCategory.STOCKS),
            Pair(chipHeroIndices, MarketCategory.INDICES),
            Pair(chipHeroForex, MarketCategory.FOREX)
        )
        val activeColor = ContextCompat.getColor(this, R.color.accent_cyan)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        chipMap.forEach { (chip, cat) ->
            if (cat == activeCategory) {
                chip.setBackgroundResource(R.drawable.bg_chip_cyan_active)
                chip.setTextColor(activeColor)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive)
                chip.setTextColor(inactiveColor)
            }
        }
    }

    private fun updateTimeRangeButtonsStyle(activeRange: String) {
        val rangeButtons = listOf(
            Pair(btnHero1D, "1D"),
            Pair(btnHero1W, "1W"),
            Pair(btnHero1M, "1M"),
            Pair(btnHero3M, "3M"),
            Pair(btnHero1Y, "1Y"),
            Pair(btnHeroALL, "ALL"),
            Pair(btnHeroCustomDate, "CUSTOM")
        )
        val activeColor = ContextCompat.getColor(this, R.color.accent_cyan)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        rangeButtons.forEach { (btn, range) ->
            if (range == activeRange) {
                btn.setBackgroundResource(R.drawable.bg_chip_cyan_active)
                btn.setTextColor(activeColor)
            } else {
                btn.setBackgroundResource(R.drawable.bg_chip_inactive)
                btn.setTextColor(inactiveColor)
            }
        }
    }

    private fun updateIndicatorButtonsStyle(isMa: Boolean, isEma: Boolean, isRsi: Boolean, isMacd: Boolean) {
        val activeColor = ContextCompat.getColor(this, R.color.accent_cyan)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        btnIndicatorMa.setBackgroundResource(if (isMa) R.drawable.bg_chip_cyan_active else R.drawable.bg_chip_inactive)
        btnIndicatorMa.setTextColor(if (isMa) activeColor else inactiveColor)

        btnIndicatorEma.setBackgroundResource(if (isEma) R.drawable.bg_chip_cyan_active else R.drawable.bg_chip_inactive)
        btnIndicatorEma.setTextColor(if (isEma) activeColor else inactiveColor)

        btnIndicatorRsi.setBackgroundResource(if (isRsi) R.drawable.bg_chip_cyan_active else R.drawable.bg_chip_inactive)
        btnIndicatorRsi.setTextColor(if (isRsi) activeColor else inactiveColor)

        btnIndicatorMacd.setBackgroundResource(if (isMacd) R.drawable.bg_chip_cyan_active else R.drawable.bg_chip_inactive)
        btnIndicatorMacd.setTextColor(if (isMacd) activeColor else inactiveColor)
    }

    private fun renderHeroChart(
        activeInstrument: MarketInstrument,
        timeRange: String,
        startDateMs: Long?,
        endDateMs: Long?,
        isMaEnabled: Boolean,
        isEmaEnabled: Boolean,
        isRsiEnabled: Boolean,
        isMacdEnabled: Boolean
    ) {
        val repository = com.repp.aquaticremineztools.data.repository.MarketRepository.getInstance(this)
        val rawEntries = repository.getHistoricalEntriesForHero(activeInstrument, timeRange, startDateMs, endDateMs)

        tvHeroSymbol.text = "${activeInstrument.symbol} · ${activeInstrument.name}"

        val price = activeInstrument.currentPrice
        if (price != null) {
            val formatted = when (activeInstrument.currency) {
                "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                "%" -> String.format(Locale.US, "%.2f%%", price)
                "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                "EUR" -> String.format(Locale.US, "€%,.4f", price)
                else -> if (price >= 1.0) String.format(Locale.US, "$%,.2f", price) else String.format(Locale.US, "$%,.4f", price)
            }
            tvHeroPrice.text = formatted
        } else {
            tvHeroPrice.text = "--"
        }

        val pctChange = activeInstrument.priceChangePercentage24h
        if (pctChange != null) {
            val isPos = pctChange >= 0
            val sign = if (isPos) "+" else ""
            tvHeroChange.text = String.format(Locale.US, "%s%.2f%% (24h)", sign, pctChange)
            val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
            tvHeroChange.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            tvHeroChange.text = "--"
        }

        tvHeroDataStatus.text = when (activeInstrument.dataStatus) {
            MarketDataStatus.LIVE -> "LIVE FEED"
            MarketDataStatus.CACHED -> "OFFLINE CACHE"
            MarketDataStatus.SIMULATED -> "DEMO DATA"
            MarketDataStatus.UNAVAILABLE -> "FEED UNAVAILABLE"
        }

        if (rawEntries.isEmpty()) {
            chartHero.clear()
            return
        }

        val primaryDataSet = com.github.mikephil.charting.data.LineDataSet(rawEntries, "Price").apply {
            color = Color.parseColor("#43D9E6")
            setLineWidth(2f)
            setDrawCircles(false)
            setDrawValues(false)
            mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#43D9E6")
            fillAlpha = 35
            highLightColor = Color.parseColor("#00E5FF")
        }

        val dataSets = mutableListOf<com.github.mikephil.charting.data.LineDataSet>(primaryDataSet)
        val prices = rawEntries.map { it.y }

        if (isMaEnabled) {
            val ma20 = com.repp.aquaticremineztools.core.util.TechnicalIndicators.calculateSMA(prices, 20)
            val maEntries = mutableListOf<com.github.mikephil.charting.data.Entry>()
            for (i in rawEntries.indices) {
                val valMa = ma20[i]
                if (valMa != null) {
                    maEntries.add(com.github.mikephil.charting.data.Entry(i.toFloat(), valMa))
                }
            }
            if (maEntries.isNotEmpty()) {
                val maDataSet = com.github.mikephil.charting.data.LineDataSet(maEntries, "MA(20)").apply {
                    color = Color.parseColor("#FFD700")
                    setLineWidth(1.5f)
                    setDrawCircles(false)
                    setDrawValues(false)
                }
                dataSets.add(maDataSet)
            }
        }

        if (isEmaEnabled) {
            val ema50 = com.repp.aquaticremineztools.core.util.TechnicalIndicators.calculateEMA(prices, 50)
            val emaEntries = mutableListOf<com.github.mikephil.charting.data.Entry>()
            for (i in rawEntries.indices) {
                val valEma = ema50[i]
                if (valEma != null) {
                    emaEntries.add(com.github.mikephil.charting.data.Entry(i.toFloat(), valEma))
                }
            }
            if (emaEntries.isNotEmpty()) {
                val emaDataSet = com.github.mikephil.charting.data.LineDataSet(emaEntries, "EMA(50)").apply {
                    color = Color.parseColor("#8B5CF6")
                    setLineWidth(1.5f)
                    setDrawCircles(false)
                    setDrawValues(false)
                }
                dataSets.add(emaDataSet)
            }
        }

        val indicatorTextList = mutableListOf<String>()
        if (isRsiEnabled) {
            val rsiVal = com.repp.aquaticremineztools.core.util.TechnicalIndicators.calculateRSI(prices, 14)
            if (rsiVal != null) {
                val rsiState = when {
                    rsiVal >= 70 -> "Overbought"
                    rsiVal <= 30 -> "Oversold"
                    else -> "Neutral"
                }
                indicatorTextList.add(String.format(Locale.US, "RSI(14): %.1f (%s)", rsiVal, rsiState))
            }
        }

        if (isMacdEnabled) {
            val macdPair = com.repp.aquaticremineztools.core.util.TechnicalIndicators.calculateMACD(prices)
            if (macdPair != null) {
                val macdState = if (macdPair.first >= macdPair.second) "Bullish" else "Bearish"
                indicatorTextList.add(String.format(Locale.US, "MACD: %+.2f (%s)", macdPair.first, macdState))
            }
        }

        if (indicatorTextList.isNotEmpty()) {
            tvIndicatorStatus.text = indicatorTextList.joinToString(" · ")
            tvIndicatorStatus.visibility = View.VISIBLE
        } else {
            tvIndicatorStatus.visibility = View.GONE
        }

        val lineData = com.github.mikephil.charting.data.LineData(dataSets as List<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>)
        chartHero.data = lineData

        chartHero.description.isEnabled = false
        chartHero.legend.isEnabled = false
        chartHero.setTouchEnabled(true)
        chartHero.isDragEnabled = true
        chartHero.setScaleEnabled(false)

        val xAxis = chartHero.xAxis
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.parseColor("#80FFFFFF")
        xAxis.setLabelCount(4, false)

        val yAxisLeft = chartHero.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = Color.parseColor("#1AFFFFFF")
        yAxisLeft.textColor = Color.parseColor("#80FFFFFF")

        val yAxisRight = chartHero.axisRight
        yAxisRight.isEnabled = false

        val markerView = MarketChartMarkerView(
            this,
            R.layout.layout_chart_marker_view,
            when (activeInstrument.currency) {
                "IDR" -> "Rp"
                "JPY" -> "¥"
                "%" -> "%"
                else -> "$"
            }
        )
        markerView.chartView = chartHero
        chartHero.marker = markerView

        chartHero.invalidate()
    }

    // ============================
    // NEWS WIDGET
    // ============================
    private fun setupNewsSection() {
        findViewById<View>(R.id.btnNewsRefresh)?.setOnClickListener {
            newsViewModel.loadHomepageNews(this, true)
            Toast.makeText(this, "Refreshing News...", Toast.LENGTH_SHORT).show()
        }

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
            ivFeaturedThumbnail.visibility = View.VISIBLE
            ivFeaturedThumbnail.load(featuredArticle.thumbnailUrl) {
                crossfade(true)
            }
        } else {
            ivFeaturedThumbnail.setImageDrawable(null)
        }

        featuredView.setOnClickListener {
            openNewsDetail(featuredArticle)
        }

        containerFeaturedArticle.addView(featuredView)

        val compactList = articles.filter { it.articleUrl != featuredArticle.articleUrl }.take(4)
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
                ivCompactThumbnail.visibility = View.VISIBLE
                ivCompactThumbnail.load(article.thumbnailUrl) {
                    crossfade(true)
                }
            } else {
                ivCompactThumbnail.setImageDrawable(null)
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

        when {
            pct >= 80 -> {
                ivBatteryIcon.setImageResource(R.drawable.ic_battery_full)
                ivBatteryIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent_teal))
            }
            pct >= 50 -> {
                ivBatteryIcon.setImageResource(R.drawable.ic_battery_half)
                ivBatteryIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent_teal))
            }
            pct >= 20 -> {
                ivBatteryIcon.setImageResource(R.drawable.ic_battery_low)
                ivBatteryIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_warning))
            }
            else -> {
                ivBatteryIcon.setImageResource(R.drawable.ic_battery_low)
                ivBatteryIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent_red))
            }
        }
    }

    // ============================
    // LOCATION & WEATHER
    // ============================
    private fun requestLocationAndWeather() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_CODE)
        } else {
            fetchLocationAndWeather()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        } else {
            tvLocation.text = "Location Disabled"
            tvTemperature.text = "--°C"
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndWeather() {
        val now = System.currentTimeMillis()
        if (now - lastLocationFetch < LOCATION_REFRESH_INTERVAL && lastLocationFetch != 0L) {
            return
        }
        lastLocationFetch = now

        executor.execute {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if (loc != null) {
                    val lat = loc.latitude
                    val lon = loc.longitude

                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)
                    val cityName = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: "Unknown City"

                    val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        val current = json.getJSONObject("current_weather")
                        val temp = current.getDouble("temperature")
                        val code = current.getInt("weathercode")

                        val icon = when (code) {
                            0 -> "☀️"
                            1, 2, 3 -> "⛅"
                            45, 48 -> "🌫️"
                            51, 53, 55, 61, 63, 65 -> "🌧️"
                            95, 96, 99 -> "⛈️"
                            else -> "🌤️"
                        }

                        runOnUiThread {
                            tvWeatherIcon.text = icon
                            tvTemperature.text = "${temp.roundToInt()}°C"
                            tvLocation.text = cleanLocationName(cityName)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Weather fetch error: ${e.message}")
            }
        }
    }

    private fun cleanLocationName(rawName: String): String {
        var name = rawName
        val prefixes = listOf("Kecamatan", "Kabupaten", "Kota", "Desa", "Kelurahan", "Kec", "Kab", "Kab.")
        for (prefix in prefixes) {
            val regex = Regex("(?i)\\b$prefix\\b\\.?\\s*")
            name = name.replace(regex, "")
        }
        return name.trim()
    }

    override fun onResume() {
        super.onResume()
        cardPlayer?.playWhenReady = true
        updatePasswordLabel()
    }

    override fun onPause() {
        super.onPause()
        cardPlayer?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        pingHandler.removeCallbacks(pingRunnable)
        prayerCountdownHandler.removeCallbacks(prayerCountdownRunnable)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        cardPlayer?.release()
        executor.shutdown()
    }
}
