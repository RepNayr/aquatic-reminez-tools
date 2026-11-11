package com.repp.aquaticremineztools.presentation.video

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.local.video.VideoDownloadEntity
import com.repp.aquaticremineztools.data.local.video.VideoResultEntity
import com.repp.aquaticremineztools.data.video.model.VideoEpisode

class VideoActivity : AppCompatActivity() {

    private val viewModel: VideoViewModel by viewModels()

    private lateinit var viewTopSpacer: View
    private lateinit var btnBack: FrameLayout

    private lateinit var etUrlInput: EditText
    private lateinit var btnPasteUrl: ImageView
    private lateinit var btnAnalyze: Button
    private lateinit var pbAnalyzeLoading: ProgressBar

    private lateinit var cardVideoPreview: View
    private lateinit var ivCoverPreview: ImageView
    private lateinit var tvCoverPlaceholder: TextView
    private lateinit var tvSeriesTitle: TextView
    private lateinit var tvSeriesDescription: TextView

    private lateinit var tvEpisodeHeaderCount: TextView
    private lateinit var rvEpisodeList: RecyclerView
    private lateinit var btnDownloadAll: Button

    private lateinit var tvResultsHeader: TextView
    private lateinit var rvResultsList: RecyclerView

    private lateinit var btnFloatingDownload: FrameLayout
    private lateinit var tvBadgeCounter: TextView
    private lateinit var vPopupScrim: View

    private lateinit var episodeAdapter: VideoEpisodeAdapter
    private lateinit var resultAdapter: VideoResultAdapter

    private var popupWindow: PopupWindow? = null
    private var popupAdapter: VideoDownloadAdapter? = null
    private var popupStatusBadgeText: TextView? = null
    private var popupEmptyStateText: TextView? = null

    private var currentActiveDownloads: List<VideoDownloadEntity> = emptyList()
    private var pendingSaveResult: VideoResultEntity? = null

    private val saveVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.data != null) {
            val targetUri = result.data?.data!!
            val sourcePath = pendingSaveResult?.videoPath
            if (!sourcePath.isNullOrBlank()) {
                try {
                    val sourceFile = java.io.File(sourcePath)
                    if (sourceFile.exists()) {
                        contentResolver.openOutputStream(targetUri, "w")?.use { output ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(this, "Video berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "File video tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal menyimpan video: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        pendingSaveResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        bindViews()
        setupWindowInsets()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupBackPressHandler()
    }

    private fun bindViews() {
        viewTopSpacer = findViewById(R.id.viewTopSpacer)
        btnBack = findViewById(R.id.btnBack)

        etUrlInput = findViewById(R.id.etUrlInput)
        btnPasteUrl = findViewById(R.id.btnPasteUrl)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        pbAnalyzeLoading = findViewById(R.id.pbAnalyzeLoading)

        cardVideoPreview = findViewById(R.id.cardVideoPreview)
        ivCoverPreview = findViewById(R.id.ivCoverPreview)
        tvCoverPlaceholder = findViewById(R.id.tvCoverPlaceholder)
        tvSeriesTitle = findViewById(R.id.tvSeriesTitle)
        tvSeriesDescription = findViewById(R.id.tvSeriesDescription)

        tvEpisodeHeaderCount = findViewById(R.id.tvEpisodeHeaderCount)
        rvEpisodeList = findViewById(R.id.rvEpisodeList)
        btnDownloadAll = findViewById(R.id.btnDownloadAll)

        tvResultsHeader = findViewById(R.id.tvResultsHeader)
        rvResultsList = findViewById(R.id.rvResultsList)

        btnFloatingDownload = findViewById(R.id.btnFloatingDownload)
        tvBadgeCounter = findViewById(R.id.tvBadgeCounter)
        vPopupScrim = findViewById(R.id.vPopupScrim)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootVideoLayout)) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val lp = viewTopSpacer.layoutParams
            if (lp.height != statusBarHeight) {
                lp.height = statusBarHeight
                viewTopSpacer.layoutParams = lp
            }
            insets
        }
    }

    private fun setupRecyclerView() {
        episodeAdapter = VideoEpisodeAdapter(
            onEpisodeClick = { episode -> openEpisodeConfig(episode) },
            onDownloadClick = { episode -> downloadEpisode(episode) }
        )
        rvEpisodeList.layoutManager = LinearLayoutManager(this)
        rvEpisodeList.adapter = episodeAdapter

        resultAdapter = VideoResultAdapter(
            onPlayClick = { result -> playVideo(result.videoPath) },
            onCompressClick = { result, targetSizeBytes ->
                viewModel.compressResult(result, targetSizeBytes)
                Toast.makeText(this, "Pekerjaan kompresi video (target ${targetSizeBytes / (1024 * 1024)} MB) ditambahkan ke antrean.", Toast.LENGTH_SHORT).show()
            },
            onSaveClick = { result -> saveVideo(result) },
            onRemoveClick = { result -> showRemoveResultDialog(result) }
        )
        rvResultsList.layoutManager = LinearLayoutManager(this)
        rvResultsList.adapter = resultAdapter
    }

    private fun setupObservers() {
        viewModel.analyzeState.observe(this) { state ->
            when (state) {
                is AnalyzeVideoState.Idle -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                }
                is AnalyzeVideoState.Analyzing -> {
                    pbAnalyzeLoading.visibility = View.VISIBLE
                    btnAnalyze.isEnabled = false
                }
                is AnalyzeVideoState.Success -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true

                    val series = state.series
                    cardVideoPreview.visibility = View.VISIBLE
                    tvSeriesTitle.text = series.title
                    tvSeriesDescription.text = series.description

                    if (series.coverUrl.isNotBlank()) {
                        ivCoverPreview.load(series.coverUrl) { crossfade(true) }
                        tvCoverPlaceholder.visibility = View.GONE
                    } else {
                        tvCoverPlaceholder.visibility = View.VISIBLE
                    }

                    tvEpisodeHeaderCount.visibility = View.VISIBLE
                    tvEpisodeHeaderCount.text = "${series.episodes.size} Episodes"

                    episodeAdapter.setEpisodes(series.episodes, series.coverUrl)
                    btnDownloadAll.visibility = if (series.episodes.isNotEmpty()) View.VISIBLE else View.GONE
                }
                is AnalyzeVideoState.Error -> {
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
            updatePopupWindowPosition()
        }

        viewModel.videoResults.observe(this) { list ->
            if (list.isNullOrEmpty()) {
                tvResultsHeader.visibility = View.GONE
                rvResultsList.visibility = View.GONE
            } else {
                tvResultsHeader.visibility = View.VISIBLE
                rvResultsList.visibility = View.VISIBLE
                resultAdapter.setItems(list)
            }
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
            onDownloadAllClicked()
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

    private fun openEpisodeConfig(episode: VideoEpisode) {
        val savedConfig = viewModel.getSavedConfig(episode.id)
        val sheet = EpisodeConfigBottomSheet.newInstance(
            episode = episode,
            initialServerId = savedConfig?.first,
            initialQualityLabel = savedConfig?.second,
            onSaveConfig = { serverId, qualityLabel ->
                viewModel.saveEpisodeConfig(episode.id, serverId, qualityLabel)
                Toast.makeText(this, "Pengaturan ${episode.title} disimpan: $qualityLabel", Toast.LENGTH_SHORT).show()
            }
        )
        sheet.show(supportFragmentManager, "EpisodeConfigBottomSheet")
    }

    private fun downloadEpisode(episode: VideoEpisode) {
        val config = viewModel.getSavedConfig(episode.id)
        val default720 = episode.availableSources.find { it.quality.height == 720 }
        if (config == null && default720 == null) {
            AlertDialog.Builder(this)
                .setTitle("Konfigurasi Diperlukan")
                .setMessage("Episode '${episode.title}' belum memiliki resolusi valid (720p tidak tersedia). Silakan ketuk baris episode untuk memilih resolusi terlebih dahulu.")
                .setPositiveButton("Buka Pengaturan") { _, _ -> openEpisodeConfig(episode) }
                .setNegativeButton("Batal", null)
                .show()
            return
        }

        viewModel.downloadEpisode(episode) { enqueued ->
            if (enqueued) {
                Toast.makeText(this, "Menambahkan ${episode.title} ke antrean download", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "${episode.title} sudah berada di antrean atau selesai", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onDownloadAllClicked() {
        val unconfigured = viewModel.getUnconfiguredEpisodes()
        if (unconfigured.isNotEmpty()) {
            val titles = unconfigured.joinToString("\n• ") { it.title }
            AlertDialog.Builder(this)
                .setTitle("Konfigurasi Resolusi Belum Lengkap")
                .setMessage("Episode berikut tidak menyediakan resolusi default 720p dan belum dikonfigurasi secara manual:\n\n• $titles\n\nHarap atur resolusi episode tersebut terlebih dahulu sebelum menggunakan Download All.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        viewModel.downloadAllEpisodes { addedCount ->
            if (addedCount > 0) {
                Toast.makeText(this, "Menambahkan $addedCount episode ke antrean download", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Semua episode sudah berada di antrean atau selesai", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveVideo(result: VideoResultEntity) {
        pendingSaveResult = result
        val safeTitle = "${result.seriesTitle}_${result.episodeTitle}_${result.resolutionLabel}".replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/mp4"
            putExtra(Intent.EXTRA_TITLE, "$safeTitle.mp4")
        }
        try {
            saveVideoLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka penyimpan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo(videoPath: String) {
        val file = java.io.File(videoPath)
        if (!file.exists()) {
            Toast.makeText(this, "File video tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memutar video: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRemoveResultDialog(result: VideoResultEntity) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Result Video")
            .setMessage("Hapus '${result.episodeTitle}' dari daftar result dan file lokal?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteResult(result.id)
                Toast.makeText(this, "Result video dihapus.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
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

        popupAdapter = VideoDownloadAdapter(
            onCancelClick = { itemToCancel ->
                if (itemToCancel.status == VideoDownloadEntity.STATUS_FAILED || itemToCancel.status == VideoDownloadEntity.STATUS_CANCELLED) {
                    viewModel.deleteDownload(itemToCancel.id)
                } else {
                    viewModel.cancelDownload(itemToCancel.id)
                }
            },
            onRetryClick = { itemToRetry ->
                viewModel.cancelDownload(itemToRetry.id)
            }
        )
        rvPopupList.layoutManager = LinearLayoutManager(this)
        rvPopupList.adapter = popupAdapter

        popupAdapter?.setItems(currentActiveDownloads)
        updatePopupBadgeHeader(currentActiveDownloads)

        btnClose.setOnClickListener {
            popupWindow?.dismiss()
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val popupWidth = (screenWidth - 48).coerceAtLeast(280)

        popupWindow = PopupWindow(
            popupView,
            popupWidth,
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
            View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels / 2, View.MeasureSpec.AT_MOST)
        )

        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        btnFloatingDownload.getLocationOnScreen(location)
        val xPos = (screenWidth - popupWidth) / 2
        val yPos = (location[1] - popupHeight - 16).coerceAtLeast(16)

        popupWindow?.showAtLocation(btnFloatingDownload, Gravity.NO_GRAVITY, xPos, yPos)
    }

    private fun updatePopupBadgeHeader(list: List<VideoDownloadEntity>) {
        val activeCount = list.count { it.status == VideoDownloadEntity.STATUS_DOWNLOADING }
        val queuedCount = list.count { it.status == VideoDownloadEntity.STATUS_QUEUED || it.status == VideoDownloadEntity.STATUS_FAILED }

        val badgeStr = when {
            activeCount > 0 && queuedCount > 0 -> "$activeCount ACTIVE · $queuedCount QUEUED/FAILED"
            activeCount > 0 -> "$activeCount ACTIVE"
            queuedCount > 0 -> "$queuedCount QUEUED/FAILED"
            else -> "NO ACTIVE DOWNLOADS"
        }

        popupStatusBadgeText?.text = badgeStr
        popupEmptyStateText?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        popupEmptyStateText?.text = "No active or queued video downloads"
    }

    private fun updatePopupWindowPosition() {
        val window = popupWindow ?: return
        if (!window.isShowing) return
        val contentView = window.contentView ?: return

        val screenWidth = resources.displayMetrics.widthPixels
        val popupWidth = (screenWidth - 48).coerceAtLeast(280)

        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels / 2, View.MeasureSpec.AT_MOST)
        )

        val popupHeight = contentView.measuredHeight

        val location = IntArray(2)
        btnFloatingDownload.getLocationOnScreen(location)
        val xPos = (screenWidth - popupWidth) / 2
        val yPos = (location[1] - popupHeight - 16).coerceAtLeast(16)

        window.update(xPos, yPos, popupWidth, popupHeight)
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            viewModel.cancelAnalysis()
        }
    }
}
