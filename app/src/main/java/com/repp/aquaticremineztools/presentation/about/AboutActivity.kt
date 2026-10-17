package com.repp.aquaticremineztools.presentation.about

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class AboutActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var aboutBlurTarget: BlurTarget
    private lateinit var btnMenu: FrameLayout

    // Blur Cards
    private lateinit var blurAppIdentity: BlurView
    private lateinit var blurAboutApp: BlurView
    private lateinit var blurFeatures: BlurView
    private lateinit var blurDeveloper: BlurView
    private lateinit var blurConnect: BlurView
    private lateinit var blurLegal: BlurView
    private lateinit var blurAppInfo: BlurView

    // Connect buttons
    private lateinit var btnConnectGithub: LinearLayout
    private lateinit var btnConnectTiktok: LinearLayout
    private lateinit var btnConnectInstagram: LinearLayout
    private lateinit var btnConnectEmail: LinearLayout

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_about)

        bindViews()
        setupGlassCards()
        setupBottomNav()
        setupDrawerMenuClicks()
        setupClicks()
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        aboutBlurTarget = findViewById(R.id.aboutBlurTarget)
        btnMenu = findViewById(R.id.btnMenu)

        btnConnectGithub = findViewById(R.id.btnConnectGithub)
        btnConnectTiktok = findViewById(R.id.btnConnectTiktok)
        btnConnectInstagram = findViewById(R.id.btnConnectInstagram)
        btnConnectEmail = findViewById(R.id.btnConnectEmail)

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

    private fun setupGlassCards() {
        aboutBlurTarget = findViewById(R.id.aboutBlurTarget)
        blurAppIdentity = findViewById(R.id.blurAppIdentity)
        blurAboutApp = findViewById(R.id.blurAboutApp)
        blurFeatures = findViewById(R.id.blurFeatures)
        blurDeveloper = findViewById(R.id.blurDeveloper)
        blurConnect = findViewById(R.id.blurConnect)
        blurLegal = findViewById(R.id.blurLegal)
        blurAppInfo = findViewById(R.id.blurAppInfo)

        val frameClearDrawable = window.decorView.background
        GlassBlurHelper.setupBlur(blurAppIdentity, aboutBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurAboutApp, aboutBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurFeatures, aboutBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurDeveloper, aboutBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurConnect, aboutBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurLegal, aboutBlurTarget, frameClearDrawable)
        GlassBlurHelper.setupBlur(blurAppInfo, aboutBlurTarget, frameClearDrawable)
    }

    private fun setupClicks() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnConnectGithub.setOnClickListener {
            openUrl("https://github.com/RepNayr")
        }

        val notAvailableMsg = getString(R.string.about_status_unavailable)
        btnConnectTiktok.setOnClickListener {
            Toast.makeText(this, "TikTok: $notAvailableMsg", Toast.LENGTH_SHORT).show()
        }
        btnConnectInstagram.setOnClickListener {
            Toast.makeText(this, "Instagram: $notAvailableMsg", Toast.LENGTH_SHORT).show()
        }
        btnConnectEmail.setOnClickListener {
            Toast.makeText(this, "Email: $notAvailableMsg", Toast.LENGTH_SHORT).show()
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

    private fun setupBottomNav() {
        selectNavTab(3) // Index 3 is About

        tabHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
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
            selectNavTab(3)
        }

        tabSettings.setOnClickListener {
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
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
        findViewById<LinearLayout>(R.id.menuHome).setNavigatorClick {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuSearch).setNavigatorClick {
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuHistory).setNavigatorClick {
            val intent = Intent(this, HistoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        findViewById<LinearLayout>(R.id.menuFavorites).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Favorites - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuCategories).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Categories - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuStorage).setNavigatorClick {
            closeDrawer()
            ActivityTransition.startActivitySmooth(this, Intent(this, StorageActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.menuAbout).setNavigatorClick {
            closeDrawer()
        }

        findViewById<LinearLayout>(R.id.menuSettings).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun View.setNavigatorClick(action: () -> Unit) {
        setOnClickListener {
            closeDrawer()
            action()
        }
    }

    private fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectNavTab(3)
    }

    override fun onResume() {
        super.onResume()
        selectNavTab(3)
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
