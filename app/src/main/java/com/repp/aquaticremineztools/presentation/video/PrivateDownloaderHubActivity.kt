package com.repp.aquaticremineztools.presentation.video

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.presentation.manga.MangaActivity

class PrivateDownloaderHubActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var cardMangaDownloader: LinearLayout
    private lateinit var cardVideoDownloader: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_downloader_hub)

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        cardMangaDownloader = findViewById(R.id.cardMangaDownloader)
        cardVideoDownloader = findViewById(R.id.cardVideoDownloader)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        cardMangaDownloader.setOnClickListener {
            val intent = Intent(this, MangaActivity::class.java)
            startActivity(intent)
        }

        cardVideoDownloader.setOnClickListener {
            val intent = Intent(this, VideoActivity::class.java)
            startActivity(intent)
        }
    }
}
