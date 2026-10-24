package com.repp.aquaticremineztools.presentation.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import coil.load
import com.google.android.material.button.MaterialButton
import com.repp.aquaticremineztools.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        parseIntentExtras()
        bindViews()

        findViewById<NestedScrollView>(R.id.scrollReaderContent).post {
            findViewById<NestedScrollView>(R.id.scrollReaderContent).scrollTo(0, 0)
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
        val btnOpenBrowser = findViewById<ImageView>(R.id.btnReaderOpenBrowser)
        val ivImage = findViewById<ImageView>(R.id.ivReaderImage)
        val tvCategory = findViewById<TextView>(R.id.tvReaderCategory)
        val tvHeadline = findViewById<TextView>(R.id.tvReaderHeadline)
        val tvPublisherMeta = findViewById<TextView>(R.id.tvReaderPublisherMeta)
        val tvBodyText = findViewById<TextView>(R.id.tvReaderBodyText)
        val tvAttribution = findViewById<TextView>(R.id.tvReaderAttribution)
        val btnOpenOriginalUrl = findViewById<MaterialButton>(R.id.btnOpenOriginalUrl)

        btnBack.setOnClickListener { finish() }

        btnShare.setOnClickListener { shareArticle() }

        btnOpenBrowser.setOnClickListener { openInExternalBrowser() }
        btnOpenOriginalUrl.setOnClickListener { openInExternalBrowser() }

        tvCategory.text = category.uppercase(Locale.ROOT)
        tvHeadline.text = headline

        val fullDate = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date(publishedEpoch))
        val relativeTime = DateUtils.getRelativeTimeSpanString(publishedEpoch, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        tvPublisherMeta.text = "$publisher · $fullDate ($relativeTime)"

        val textToDisplay = if (!content.isNullOrBlank()) content!! else summary
        tvBodyText.text = textToDisplay

        tvAttribution.text = getString(R.string.news_publisher_attribution, publisher)

        if (!thumbnailUrl.isNullOrBlank()) {
            ivImage.visibility = View.VISIBLE
            ivImage.load(thumbnailUrl) {
                placeholder(R.drawable.home_background_image)
                error(R.drawable.home_background_image)
                crossfade(true)
            }
        } else {
            ivImage.visibility = View.GONE
        }
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
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }
}
