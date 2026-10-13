package com.repp.aquaticremineztools.presentation.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.data.QuickCategory
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.security.PinChangeActivity
import com.repp.aquaticremineztools.presentation.security.PinSetupActivity
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
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

    private lateinit var tvPasswordLabel: TextView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        // FORCE REFRESH saat app buka
        lastLocationFetch = 0L

        bindViews()
        setupClicks()
        setupDrawerMenuClicks()
        setupVideo()
        setupMuteButton()
        setupQuickMenu()
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

        tvPasswordLabel = findViewById(R.id.tvPasswordLabel)
    }

    private fun setupClicks() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        btnNotif.setOnClickListener { /* TODO */ }
        btnPower.setOnClickListener { finishAffinity() }
    }

    private fun setupDrawerMenuClicks() {
        findViewById<LinearLayout>(R.id.menuHome).setOnClickListener {
            closeDrawer()
        }

        findViewById<LinearLayout>(R.id.menuSearch).setOnClickListener {
            closeDrawer()
            startActivity(Intent(this, SearchActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.menuHistory).setOnClickListener {
            closeDrawer()
            Toast.makeText(this, "History - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuFavorites).setOnClickListener {
            closeDrawer()
            Toast.makeText(this, "Favorites - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuCategories).setOnClickListener {
            closeDrawer()
            Toast.makeText(this, "Categories - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuTiktok).setOnClickListener {
            openUrl(getString(R.string.drawer_tiktok_url))
        }

        findViewById<LinearLayout>(R.id.menuInstagram).setOnClickListener {
            openUrl(getString(R.string.drawer_instagram_url))
        }

        findViewById<LinearLayout>(R.id.menuGames).setOnClickListener {
            closeDrawer()
            Toast.makeText(this, "Games - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuStorage).setOnClickListener {
            closeDrawer()
            startActivity(Intent(this, StorageActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.menuPassword).setOnClickListener {
            closeDrawer()
            if (PinManager.hasPin(this)) {
                showPasswordOptionsDialog()
            } else {
                startActivity(Intent(this, PinSetupActivity::class.java))
            }
        }

        findViewById<LinearLayout>(R.id.menuSettings).setOnClickListener {
            closeDrawer()
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }
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

    // ============================
    // QUICK MENU (Batch 6)
    // ============================
    private fun setupQuickMenu() {
        val rvQuickMenu: RecyclerView = findViewById(R.id.rvQuickMenu)
        val categories = QuickCategory.getAll()

        val adapter = QuickCategoryAdapter(categories) { category ->
            // TODO Batch 7: Buka Category Page
            Toast.makeText(
                this,
                "${category.emoji} ${getString(category.nameRes)}\nComing soon!",
                Toast.LENGTH_SHORT,
            ).show()
        }

        rvQuickMenu.layoutManager = GridLayoutManager(this, 4)
        rvQuickMenu.adapter = adapter
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
                tvTemperature.text = "--°C"
                tvLocation.text = "Location Off"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndWeather() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )

            var lastLocation: Location? = null
            for (provider in providers) {
                try {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        lastLocation = loc
                        break
                    }
                } catch (_: Exception) { }
            }

            val isFresh = lastLocation != null && (System.currentTimeMillis() - lastLocation.time < 120_000)

            if (isFresh && lastLocation != null) {
                val lat = lastLocation.latitude
                val lon = lastLocation.longitude

                executor.execute {
                    val cityName = getCityName(lat, lon)
                    runOnUiThread { tvLocation.text = cityName }
                }
                fetchWeather(lat, lon)
            } else {
                lm.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    object : android.location.LocationListener {
                        override fun onLocationChanged(location: Location) {
                            val lat = location.latitude
                            val lon = location.longitude

                            executor.execute {
                                val cityName = getCityName(lat, lon)
                                runOnUiThread { tvLocation.text = cityName }
                            }
                            fetchWeather(lat, lon)
                            lm.removeUpdates(this)
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    Looper.getMainLooper(),
                )
            }
        } catch (e: Exception) {
            Log.e("WEATHER_DEBUG", "Error lokasi: ${e.message}", e)
            tvTemperature.text = "--°C"
            tvLocation.text = "Unknown"
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

        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastLocationFetch > LOCATION_REFRESH_INTERVAL) {
            requestLocationAndWeather()
        }
    }

    override fun onPause() {
        super.onPause()
        cardPlayer?.pause()
        AppLockManager.onActivityPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        cardPlayer?.release()
        cardPlayer = null
        executor.shutdown()
    }
}