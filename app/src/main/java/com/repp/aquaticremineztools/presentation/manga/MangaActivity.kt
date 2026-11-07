package com.repp.aquaticremineztools.presentation.manga

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.manga.model.MangaChapter

class MangaActivity : AppCompatActivity() {

    private val viewModel: MangaViewModel by viewModels()

    private lateinit var btnBack: FrameLayout
    private lateinit var etUrlInput: EditText
    private lateinit var btnPasteUrl: ImageView
    private lateinit var btnAnalyze: Button
    private lateinit var pbAnalyzeLoading: ProgressBar

    private lateinit var cardMangaPreview: View
    private lateinit var ivCoverPreview: ImageView
    private lateinit var tvCoverPlaceholder: TextView
    private lateinit var tvMangaTitle: TextView

    private lateinit var rvChapterList: RecyclerView
    private lateinit var btnDownloadAll: Button

    private lateinit var btnFloatingDownload: FrameLayout
    private lateinit var tvBadgeCounter: TextView
    private lateinit var vPopupScrim: View

    private lateinit var chapterAdapter: MangaChapterAdapter

    private var popupWindow: PopupWindow? = null
    private var popupAdapter: MangaDownloadAdapter? = null
    private var popupStatusBadgeText: TextView? = null
    private var popupEmptyStateText: TextView? = null

    private var currentActiveDownloads: List<MangaDownloadEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga)

        bindViews()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupBackPressHandler()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        etUrlInput = findViewById(R.id.etUrlInput)
        btnPasteUrl = findViewById(R.id.btnPasteUrl)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        pbAnalyzeLoading = findViewById(R.id.pbAnalyzeLoading)

        cardMangaPreview = findViewById(R.id.cardMangaPreview)
        ivCoverPreview = findViewById(R.id.ivCoverPreview)
        tvCoverPlaceholder = findViewById(R.id.tvCoverPlaceholder)
        tvMangaTitle = findViewById(R.id.tvMangaTitle)

        rvChapterList = findViewById(R.id.rvChapterList)
        btnDownloadAll = findViewById(R.id.btnDownloadAll)

        btnFloatingDownload = findViewById(R.id.btnFloatingDownload)
        tvBadgeCounter = findViewById(R.id.tvBadgeCounter)
        vPopupScrim = findViewById(R.id.vPopupScrim)
    }

    private fun setupRecyclerView() {
        chapterAdapter = MangaChapterAdapter { chapter ->
            onDownloadChapterClicked(chapter)
        }
        rvChapterList.layoutManager = LinearLayoutManager(this)
        rvChapterList.adapter = chapterAdapter
    }

    private fun setupObservers() {
        viewModel.analyzeState.observe(this) { state ->
            when (state) {
                is AnalyzeState.Idle -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                }
                is AnalyzeState.Loading -> {
                    pbAnalyzeLoading.visibility = View.VISIBLE
                    btnAnalyze.isEnabled = false
                }
                is AnalyzeState.Success -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true

                    val series = state.series
                    cardMangaPreview.visibility = View.VISIBLE
                    tvMangaTitle.text = series.title

                    if (series.coverUrl.isNotBlank()) {
                        ivCoverPreview.load(series.coverUrl) {
                            crossfade(true)
                        }
                        tvCoverPlaceholder.visibility = View.GONE
                    } else {
                        tvCoverPlaceholder.visibility = View.VISIBLE
                    }

                    chapterAdapter.setChapters(series.chapters)
                    btnDownloadAll.visibility = if (series.chapters.isNotEmpty()) View.VISIBLE else View.GONE
                }
                is AnalyzeState.Error -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.activeDownloads.observe(this) { list ->
            currentActiveDownloads = list
            popupAdapter?.setItems(list)
            updatePopupBadgeHeader(list)
        }

        viewModel.activeAndQueuedCount.observe(this) { count ->
            if (count > 0) {
                tvBadgeCounter.visibility = View.VISIBLE
                tvBadgeCounter.text = count.toString()
            } else {
                tvBadgeCounter.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnPasteUrl.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                etUrlInput.setText(text)
            }
        }

        btnAnalyze.setOnClickListener {
            val url = etUrlInput.text.toString().trim()
            viewModel.analyzeUrl(url)
        }

        btnDownloadAll.setOnClickListener {
            viewModel.downloadAllChapters { addedCount ->
                if (addedCount > 0) {
                    Toast.makeText(this, "Menambahkan $addedCount chapter ke antrean", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Semua chapter sudah berada di antrean atau selesai", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnFloatingDownload.setOnClickListener {
            toggleDownloadPopup()
        }

        vPopupScrim.setOnClickListener {
            popupWindow?.dismiss()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (popupWindow?.isShowing == true) {
                    popupWindow?.dismiss()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun onDownloadChapterClicked(chapter: MangaChapter) {
        viewModel.downloadChapter(chapter) { enqueued ->
            if (enqueued) {
                Toast.makeText(this, "Menambahkan ${chapter.title} ke antrean download", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "${chapter.title} sudah berada di antrean atau selesai", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleDownloadPopup() {
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            return
        }

        showDownloadPopup()
    }

    private fun showDownloadPopup() {
        val popupView = LayoutInflater.from(this).inflate(R.layout.view_download_popup, null)

        val btnClose = popupView.findViewById<ImageView>(R.id.btnPopupClose)
        popupStatusBadgeText = popupView.findViewById(R.id.tvPopupStatusBadge)
        popupEmptyStateText = popupView.findViewById(R.id.tvPopupEmptyState)
        val rvPopupList = popupView.findViewById<RecyclerView>(R.id.rvDownloadPopupList)

        popupAdapter = MangaDownloadAdapter { itemToCancel ->
            viewModel.cancelDownload(itemToCancel.id)
        }
        rvPopupList.layoutManager = LinearLayoutManager(this)
        rvPopupList.adapter = popupAdapter

        popupAdapter?.setItems(currentActiveDownloads)
        updatePopupBadgeHeader(currentActiveDownloads)

        btnClose.setOnClickListener {
            popupWindow?.dismiss()
        }

        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isFocusable = true
            setOnDismissListener {
                vPopupScrim.visibility = View.GONE
            }
        }

        vPopupScrim.visibility = View.VISIBLE

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        btnFloatingDownload.getLocationOnScreen(location)
        val xPos = location[0]
        val yPos = location[1] - popupHeight - 16

        popupWindow?.showAtLocation(btnFloatingDownload, Gravity.NO_GRAVITY, xPos.coerceAtLeast(16), yPos)
    }

    private fun updatePopupBadgeHeader(list: List<MangaDownloadEntity>) {
        val activeCount = list.count { it.status == MangaDownloadEntity.STATUS_DOWNLOADING }
        val queuedCount = list.count { it.status == MangaDownloadEntity.STATUS_QUEUED }

        val badgeStr = when {
            activeCount > 0 && queuedCount > 0 -> "$activeCount ACTIVE · $queuedCount QUEUED"
            activeCount > 0 -> "$activeCount ACTIVE"
            queuedCount > 0 -> "$queuedCount QUEUED"
            else -> "NO ACTIVE DOWNLOADS"
        }

        popupStatusBadgeText?.text = badgeStr
        popupEmptyStateText?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
}
