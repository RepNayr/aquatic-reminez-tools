package com.repp.aquaticremineztools.presentation.manga

import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import com.repp.aquaticremineztools.data.manga.source.CrotPediaCookieJar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class CrotPediaLoginActivity : AppCompatActivity() {

    private lateinit var viewLoginTopSpacer: View
    private lateinit var btnLoginBack: FrameLayout
    private lateinit var btnWebViewRetry: Button
    private lateinit var pbWebViewLoading: ProgressBar
    private lateinit var webViewCrotPedia: WebView
    private lateinit var layoutVerifying: View

    private val loginUrl = "https://crotpedia.net/login/"
    private var isVerifying = false

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(CrotPediaCookieJar(this))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crotpedia_login)

        bindViews()
        setupWindowInsets()
        setupWebView()
        setupListeners()
        setupBackPressHandler()

        webViewCrotPedia.loadUrl(loginUrl)
    }

    private fun bindViews() {
        viewLoginTopSpacer = findViewById(R.id.viewLoginTopSpacer)
        btnLoginBack = findViewById(R.id.btnLoginBack)
        btnWebViewRetry = findViewById(R.id.btnWebViewRetry)
        pbWebViewLoading = findViewById(R.id.pbWebViewLoading)
        webViewCrotPedia = findViewById(R.id.webViewCrotPedia)
        layoutVerifying = findViewById(R.id.layoutVerifying)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLoginLayout)) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val lp = viewLoginTopSpacer.layoutParams
            if (lp.height != statusBarHeight) {
                lp.height = statusBarHeight
                viewLoginTopSpacer.layoutParams = lp
            }
            insets
        }
    }

    private fun setupWebView() {
        val settings = webViewCrotPedia.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webViewCrotPedia, true)

        webViewCrotPedia.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                pbWebViewLoading.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pbWebViewLoading.visibility = View.GONE
                if (!url.isNullOrBlank() && CrotPediaSessionStore.isAllowedCookieHost(android.net.Uri.parse(url).host)) {
                    checkLoginSuccess(url)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    pbWebViewLoading.visibility = View.GONE
                    Toast.makeText(this@CrotPediaLoginActivity, "Gagal memuat halaman. Silakan tekan Retry.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        btnLoginBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        btnWebViewRetry.setOnClickListener {
            webViewCrotPedia.reload()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webViewCrotPedia.canGoBack()) {
                    webViewCrotPedia.goBack()
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        })
    }

    private fun checkLoginSuccess(currentUrl: String) {
        if (isVerifying) return

        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(currentUrl) ?: cookieManager.getCookie("https://crotpedia.net/")

        if (!cookies.isNullOrBlank() && (cookies.contains("wordpress_logged_in_") || cookies.contains("logged_in") || cookies.contains("session") || cookies.contains("token"))) {
            isVerifying = true
            layoutVerifying.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = Request.Builder()
                        .url("https://crotpedia.net/")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Referer", "https://crotpedia.net/")
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        val html = response.body?.string() ?: ""
                        val finalUrl = response.request.url.toString()

                        val isNotLoginPage = !finalUrl.contains("/login") && !finalUrl.contains("/signin")
                        val hasLoginForm = html.contains("form[action*=/login]") || html.contains("input[name=log]") || html.contains("id=\"loginform\"")
                        val hasUserIndicator = html.contains("logout") || html.contains("profile") || html.contains("dashboard")

                        if (isNotLoginPage && !hasLoginForm && (hasUserIndicator || response.isSuccessful)) {
                            CrotPediaSessionStore.saveSession(applicationContext, cookies)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@CrotPediaLoginActivity, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                                setResult(RESULT_OK)
                                finish()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                isVerifying = false
                                layoutVerifying.visibility = View.GONE
                            }
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        isVerifying = false
                        layoutVerifying.visibility = View.GONE
                    }
                }
            }
        }
    }
}
