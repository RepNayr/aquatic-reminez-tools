package com.repp.aquaticremineztools.presentation.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.data.repository.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class NewsDetailActivity : AppCompatActivity() {

    private var articleUrl: String = ""
    private var headline: String = ""
    private var summary: String = ""
    private var content: String? = null
    private var thumbnailUrl: String? = null
    private var publisher: String = ""
    private var category: String = ""
    private var publishedEpoch: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)

        setupOnBackPressed()
        setupWindowInsets()
        parseIntentExtras()
        bindViews()
        setupReadingProgress()

        findViewById<NestedScrollView>(R.id.scrollReaderContent).post {
            findViewById<NestedScrollView>(R.id.scrollReaderContent).scrollTo(0, 0)
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(owner = this, enabled = true) {
            ActivityTransition.finishSmooth(this@NewsDetailActivity)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootNewsDetail)) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val appBar = findViewById<AppBarLayout>(R.id.appBarReader)
            val params = appBar.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = statusBarHeight
            appBar.layoutParams = params

            val scroll = findViewById<NestedScrollView>(R.id.scrollReaderContent)
            scroll.setPadding(scroll.paddingLeft, scroll.paddingTop, scroll.paddingRight, navBarHeight + 64)
            insets
        }
    }

    private fun parseIntentExtras() {
        articleUrl = intent.getStringExtra("article_url") ?: ""
        headline = intent.getStringExtra("headline") ?: ""
        summary = intent.getStringExtra("summary") ?: ""
        content = intent.getStringExtra("content")
        thumbnailUrl = intent.getStringExtra("thumbnail_url")
        publisher = intent.getStringExtra("publisher") ?: "News Publisher"
        category = intent.getStringExtra("category") ?: "News"
        publishedEpoch = intent.getLongExtra("published_epoch", System.currentTimeMillis())
    }

    private fun bindViews() {
        val btnBack = findViewById<ImageView>(R.id.btnReaderBack)
        val btnShare = findViewById<ImageView>(R.id.btnReaderShare)
        val cardImage = findViewById<MaterialCardView>(R.id.cardReaderImage)
        val ivImage = findViewById<ImageView>(R.id.ivReaderImage)
        val tvCategory = findViewById<TextView>(R.id.tvReaderCategory)
        val tvHeadline = findViewById<TextView>(R.id.tvReaderHeadline)
        val tvDeck = findViewById<TextView>(R.id.tvReaderDeck)
        val tvPublisherMeta = findViewById<TextView>(R.id.tvReaderPublisherMeta)
        val tvBodyText = findViewById<TextView>(R.id.tvReaderBodyText)
        val tvAttribution = findViewById<TextView>(R.id.tvReaderAttribution)
        val btnOpenOriginalUrl = findViewById<MaterialButton>(R.id.btnOpenOriginalUrl)

        btnBack.setOnClickListener { ActivityTransition.finishSmooth(this) }
        btnShare.setOnClickListener { shareArticle() }
        btnOpenOriginalUrl.setOnClickListener { openInExternalBrowser() }

        tvCategory.text = category.uppercase(Locale.ROOT)
        tvHeadline.text = headline

        // Display summary as deck if content is present and summary is distinct
        if (!content.isNullOrBlank() && summary.isNotBlank() && (summary.trim() != content?.trim())) {
            val formattedDeck = if (summary.contains("<") && summary.contains(">")) {
                Html.fromHtml(summary, Html.FROM_HTML_MODE_COMPACT).toString()
            } else {
                summary
            }
            tvDeck.text = formattedDeck.trim()
            tvDeck.visibility = View.VISIBLE
        } else {
            tvDeck.visibility = View.GONE
        }

        val fullDate = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date(publishedEpoch))
        val relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
            publishedEpoch,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        )
        tvPublisherMeta.text = "$publisher · $fullDate ($relativeTime)"

        // Display full content un-truncated (HTML parsed or fallback to summary)
        val rawContent = content?.takeIf { it.isNotBlank() } ?: summary
        val formattedContent = if (rawContent.contains("<") && rawContent.contains(">")) {
            Html.fromHtml(rawContent, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            rawContent
        }
        val finalInitialContent = formattedContent.ifBlank { summary }
        tvBodyText.text = finalInitialContent

        updateReadingTime(finalInitialContent)

        tvAttribution.text = getString(R.string.news_publisher_attribution, publisher)

        if (!thumbnailUrl.isNullOrBlank()) {
            cardImage.visibility = View.VISIBLE
            ivImage.load(thumbnailUrl) {
                crossfade(true)
                allowHardware(false) // ensures crisp high quality image rendering
            }
        } else {
            cardImage.visibility = View.GONE
        }

        // Fetch full article content from URL asynchronously if initial content is short
        if (articleUrl.isNotBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val repository = NewsRepository.getInstance(this@NewsDetailActivity)
                val fullText = repository.fetchFullArticleContent(articleUrl)
                if (fullText.isNotBlank() && fullText.length > finalInitialContent.length) {
                    withContext(Dispatchers.Main) {
                        val formattedFull = if (fullText.contains("<") && fullText.contains(">")) {
                            Html.fromHtml(fullText, Html.FROM_HTML_MODE_COMPACT).toString()
                        } else {
                            fullText
                        }
                        tvBodyText.text = formattedFull
                        updateReadingTime(formattedFull)
                    }
                }
            }
        }
    }

    private fun updateReadingTime(text: String) {
        val tvReadingTime = findViewById<TextView>(R.id.tvReaderReadingTime)
        val wordCount = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        if (wordCount > 0) {
            val minutes = maxOf(1, ceil(wordCount / 200.0).toInt())
            tvReadingTime.text = getString(R.string.news_reading_time, minutes)
            tvReadingTime.visibility = View.VISIBLE
        } else {
            tvReadingTime.visibility = View.GONE
        }
    }

    private fun setupReadingProgress() {
        val scrollReaderContent = findViewById<NestedScrollView>(R.id.scrollReaderContent)
        val pbReadingProgress = findViewById<ProgressBar>(R.id.pbReadingProgress)

        scrollReaderContent.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            val child = v.getChildAt(0) ?: return@OnScrollChangeListener
            val totalScroll = child.measuredHeight - v.measuredHeight
            val progress = if (totalScroll > 0) {
                ((scrollY.toFloat() / totalScroll.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            pbReadingProgress.progress = progress
        })
    }

    private fun shareArticle() {
        if (articleUrl.isBlank()) return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, headline)
            putExtra(Intent.EXTRA_TEXT, "$headline\n\nRead more at: $articleUrl")
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.news_share)))
    }

    private fun openInExternalBrowser() {
        if (articleUrl.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }
}
