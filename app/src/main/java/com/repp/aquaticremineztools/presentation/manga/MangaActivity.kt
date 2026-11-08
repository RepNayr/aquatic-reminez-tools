package com.repp.aquaticremineztools.presentation.manga

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
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
import android.widget.PopupMenu
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
import com.repp.aquaticremineztools.data.local.manga.MangaDownloadEntity
import com.repp.aquaticremineztools.data.manga.model.MangaChapter

class MangaActivity : AppCompatActivity() {

    private val viewModel: MangaViewModel by viewModels()

    private lateinit var viewTopSpacer: View
    private lateinit var btnBack: FrameLayout
    private lateinit var btnAuthIndicator: View
    private lateinit var tvAuthLabel: TextView
    private lateinit var tvAuthTime: TextView

    private lateinit var etUrlInput: EditText
    private lateinit var btnPasteUrl: ImageView
    private lateinit var btnAnalyze: Button
    private lateinit var pbAnalyzeLoading: ProgressBar

    private lateinit var cardMangaPreview: View
    private lateinit var ivCoverPreview: ImageView
    private lateinit var tvCoverPlaceholder: TextView
    private lateinit var tvMangaTitle: TextView

    private lateinit var tvChapterHeaderCount: TextView
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
    private var currentCompletedDownloads: List<MangaDownloadEntity> = emptyList()
    private var isCompletedTab: Boolean = false

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.checkSessionState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga)

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
        btnAuthIndicator = findViewById(R.id.btnAuthIndicator)
        tvAuthLabel = findViewById(R.id.tvAuthLabel)
        tvAuthTime = findViewById(R.id.tvAuthTime)

        etUrlInput = findViewById(R.id.etUrlInput)
        btnPasteUrl = findViewById(R.id.btnPasteUrl)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        pbAnalyzeLoading = findViewById(R.id.pbAnalyzeLoading)

        cardMangaPreview = findViewById(R.id.cardMangaPreview)
        ivCoverPreview = findViewById(R.id.ivCoverPreview)
        tvCoverPlaceholder = findViewById(R.id.tvCoverPlaceholder)
        tvMangaTitle = findViewById(R.id.tvMangaTitle)

        tvChapterHeaderCount = findViewById(R.id.tvChapterHeaderCount)
        rvChapterList = findViewById(R.id.rvChapterList)
        btnDownloadAll = findViewById(R.id.btnDownloadAll)

        btnFloatingDownload = findViewById(R.id.btnFloatingDownload)
        tvBadgeCounter = findViewById(R.id.tvBadgeCounter)
        vPopupScrim = findViewById(R.id.vPopupScrim)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootMangaLayout)) { _, insets ->
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
        chapterAdapter = MangaChapterAdapter(
            onDownloadClick = { chapter -> onDownloadChapterClicked(chapter) },
            onRetryClick = { chapter -> viewModel.retryChapter(chapter.id) }
        )
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
                is AnalyzeState.Analyzing -> {
                    pbAnalyzeLoading.visibility = View.VISIBLE
                    btnAnalyze.isEnabled = false

                    cardMangaPreview.visibility = View.VISIBLE
                    tvMangaTitle.text = state.seriesTitle

                    if (state.coverUrl.isNotBlank()) {
                        ivCoverPreview.load(state.coverUrl) { crossfade(true) }
                        tvCoverPlaceholder.visibility = View.GONE
                    } else {
                        tvCoverPlaceholder.visibility = View.VISIBLE
                    }

                    tvChapterHeaderCount.visibility = View.VISIBLE
                    tvChapterHeaderCount.text = state.progressText

                    chapterAdapter.setChapters(state.series.chapters)
                    btnDownloadAll.visibility = View.GONE
                }
                is AnalyzeState.Success -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true

                    val series = state.series
                    cardMangaPreview.visibility = View.VISIBLE
                    tvMangaTitle.text = series.title

                    if (series.coverUrl.isNotBlank()) {
                        ivCoverPreview.load(series.coverUrl) { crossfade(true) }
                        tvCoverPlaceholder.visibility = View.GONE
                    } else {
                        tvCoverPlaceholder.visibility = View.VISIBLE
                    }

                    tvChapterHeaderCount.visibility = View.VISIBLE
                    tvChapterHeaderCount.text = "${series.chapters.size} Chapters"

                    chapterAdapter.setChapters(series.chapters)
                    btnDownloadAll.visibility = if (series.chapters.isNotEmpty()) View.VISIBLE else View.GONE
                }
                is AnalyzeState.Error -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true

                    if (state.isInvalidUrl) {
                        showInvalidUrlDialog(state.message)
                    } else if (state.isNoChaptersFound) {
                        showNoChaptersFoundDialog(state.message)
                    } else {
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
                is AnalyzeState.ShowReanalysisConfirmation -> {
                    pbAnalyzeLoading.visibility = View.GONE
                    btnAnalyze.isEnabled = true
                    showReanalysisConfirmationDialog(state.url)
                }
            }
        }

        viewModel.sessionState.observe(this) { state ->
            when (state) {
                is SessionState.NotLoggedIn -> {
                    tvAuthLabel.text = "LOGIN"
                    tvAuthTime.visibility = View.GONE
                    btnAuthIndicator.isEnabled = true
                }
                is SessionState.Checking -> {
                    tvAuthLabel.text = "CHECKING..."
                    tvAuthTime.visibility = View.GONE
                    btnAuthIndicator.isEnabled = false
                }
                is SessionState.LoggedIn -> {
                    tvAuthLabel.text = "LOGGED IN"
                    tvAuthTime.text = state.timestampFormatted
                    tvAuthTime.visibility = if (state.timestampFormatted.isNotBlank()) View.VISIBLE else View.GONE
                    btnAuthIndicator.isEnabled = true
                }
                is SessionState.ConnectionError -> {
                    tvAuthLabel.text = "CONNECTION ERROR"
                    tvAuthTime.visibility = View.GONE
                    btnAuthIndicator.isEnabled = true
                }
            }
        }

        viewModel.activeDownloads.observe(this) { list ->
            currentActiveDownloads = list
            if (!isCompletedTab) {
                popupAdapter?.setItems(list)
                updatePopupBadgeHeader(list)
            }
        }

        viewModel.completedDownloads.observe(this) { list ->
            currentCompletedDownloads = list
            if (isCompletedTab) {
                popupAdapter?.setItems(list)
                updatePopupBadgeHeaderCompleted(list)
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

        btnAuthIndicator.setOnClickListener {
            when (viewModel.sessionState.value) {
                is SessionState.LoggedIn -> {
                    val popup = PopupMenu(this, btnAuthIndicator)
                    popup.menu.add(0, 1, 0, "Logout")
                    popup.setOnMenuItemClickListener { item ->
                        if (item.itemId == 1) {
                            viewModel.logout()
                            Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show()
                            true
                        } else {
                            false
                        }
                    }
                    popup.show()
                }
                is SessionState.ConnectionError -> {
                    viewModel.checkSessionState()
                }
                else -> {
                    loginLauncher.launch(Intent(this, CrotPediaLoginActivity::class.java))
                }
            }
        }

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

    private fun showInvalidUrlDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("URL Tidak Valid")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                etUrlInput.setText("")
            }
            .setCancelable(false)
            .show()
    }

    private fun showNoChaptersFoundDialog(message: String) {
        cardMangaPreview.visibility = View.GONE
        tvChapterHeaderCount.visibility = View.GONE
        btnDownloadAll.visibility = View.GONE
        chapterAdapter.setChapters(emptyList())

        AlertDialog.Builder(this)
            .setTitle("No Chapters Found")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showReanalysisConfirmationDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Analisis Ulang Series?")
            .setMessage("Series ini sudah pernah dianalisis. Apakah Anda ingin menganalisis ulang seluruh chapter?")
            .setPositiveButton("LANJUT") { dialog, _ ->
                dialog.dismiss()
                viewModel.analyzeUrl(url, forceReanalysis = true)
            }
            .setNegativeButton("TIDAK JADI") { dialog, _ ->
                dialog.dismiss()
                etUrlInput.setText("")
                viewModel.onCancelReanalysis()
            }
            .setCancelable(false)
            .show()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            viewModel.cancelAnalysis()
        }
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
        val btnTabActive = popupView.findViewById<TextView>(R.id.btnTabActive)
        val btnTabCompleted = popupView.findViewById<TextView>(R.id.btnTabCompleted)

        btnTabActive.text = "Active (${currentActiveDownloads.size})"
        btnTabCompleted.text = "Completed (${currentCompletedDownloads.size})"

        popupAdapter = MangaDownloadAdapter(
            onCancelClick = { itemToCancel ->
                if (isCompletedTab) {
                    viewModel.deleteDownload(itemToCancel.id)
                } else {
                    viewModel.cancelDownload(itemToCancel.id)
                }
            },
            onRetryClick = { itemToRetry ->
                viewModel.retryDownload(itemToRetry)
                Toast.makeText(this, "Mencoba mengunduh ulang ${itemToRetry.chapterTitle}", Toast.LENGTH_SHORT).show()
            },
            onOpenClick = { entity ->
                openPdf(entity.pdfPath)
            }
        )
        rvPopupList.layoutManager = LinearLayoutManager(this)
        rvPopupList.adapter = popupAdapter

        fun updateTabStyles() {
            if (isCompletedTab) {
                btnTabCompleted.setBackgroundResource(R.drawable.bg_manga_button_dark)
                btnTabCompleted.setTextColor(Color.parseColor("#FFFDF8"))
                btnTabActive.setBackgroundResource(R.drawable.bg_manga_button_secondary)
                btnTabActive.setTextColor(Color.parseColor("#292722"))
            } else {
                btnTabActive.setBackgroundResource(R.drawable.bg_manga_button_dark)
                btnTabActive.setTextColor(Color.parseColor("#FFFDF8"))
                btnTabCompleted.setBackgroundResource(R.drawable.bg_manga_button_secondary)
                btnTabCompleted.setTextColor(Color.parseColor("#292722"))
            }
        }

        btnTabActive.setOnClickListener {
            isCompletedTab = false
            updateTabStyles()
            popupAdapter?.setItems(currentActiveDownloads)
            updatePopupBadgeHeader(currentActiveDownloads)
        }

        btnTabCompleted.setOnClickListener {
            isCompletedTab = true
            updateTabStyles()
            popupAdapter?.setItems(currentCompletedDownloads)
            updatePopupBadgeHeaderCompleted(currentCompletedDownloads)
        }

        updateTabStyles()
        if (isCompletedTab) {
            popupAdapter?.setItems(currentCompletedDownloads)
            updatePopupBadgeHeaderCompleted(currentCompletedDownloads)
        } else {
            popupAdapter?.setItems(currentActiveDownloads)
            updatePopupBadgeHeader(currentActiveDownloads)
        }

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
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels - 32, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels / 2, View.MeasureSpec.AT_MOST)
        )

        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        btnFloatingDownload.getLocationOnScreen(location)
        val xPos = location[0].coerceIn(16, resources.displayMetrics.widthPixels - popupWidth - 16)
        val yPos = (location[1] - popupHeight - 16).coerceAtLeast(16)

        popupWindow?.showAtLocation(btnFloatingDownload, Gravity.NO_GRAVITY, xPos, yPos)
    }

    private fun updatePopupBadgeHeader(list: List<MangaDownloadEntity>) {
        if (isCompletedTab) {
            updatePopupBadgeHeaderCompleted(list)
            return
        }
        val activeCount = list.count { it.status == MangaDownloadEntity.STATUS_DOWNLOADING }
        val queuedCount = list.count { it.status == MangaDownloadEntity.STATUS_QUEUED || it.status == MangaDownloadEntity.STATUS_FAILED }

        val badgeStr = when {
            activeCount > 0 && queuedCount > 0 -> "$activeCount ACTIVE · $queuedCount QUEUED/FAILED"
            activeCount > 0 -> "$activeCount ACTIVE"
            queuedCount > 0 -> "$queuedCount QUEUED/FAILED"
            else -> "NO ACTIVE DOWNLOADS"
        }

        popupStatusBadgeText?.text = badgeStr
        popupEmptyStateText?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        popupEmptyStateText?.text = "No active or queued downloads"
    }

    private fun updatePopupBadgeHeaderCompleted(list: List<MangaDownloadEntity>) {
        val count = list.size
        popupStatusBadgeText?.text = "$count COMPLETED"
        popupEmptyStateText?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        popupEmptyStateText?.text = "No completed downloads yet"
    }

    private fun openPdf(pdfPath: String?) {
        if (pdfPath.isNullOrBlank()) {
            Toast.makeText(this, "File PDF tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = Uri.parse(pdfPath)
            val pfd = try {
                contentResolver.openFileDescriptor(uri, "r")
            } catch (e: Exception) {
                null
            }
            if (pfd == null) {
                Toast.makeText(this, "File PDF sudah dihapus atau URI tidak valid.", Toast.LENGTH_LONG).show()
                return
            }
            pfd.close()

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Tidak ada aplikasi pembaca PDF yang terinstal.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
