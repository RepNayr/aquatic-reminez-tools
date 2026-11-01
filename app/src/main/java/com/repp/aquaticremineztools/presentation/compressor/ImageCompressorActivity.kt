package com.repp.aquaticremineztools.presentation.compressor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.data.compressor.CloudConsentState
import com.repp.aquaticremineztools.data.compressor.CompressionConfig
import com.repp.aquaticremineztools.data.compressor.CompressionMode
import com.repp.aquaticremineztools.data.compressor.CompressionPreset
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import com.repp.aquaticremineztools.data.compressor.CompressorPreferences
import com.repp.aquaticremineztools.data.compressor.ItemStatus
import com.repp.aquaticremineztools.data.compressor.OutputFormat
import com.repp.aquaticremineztools.data.compressor.ResizeMode
import com.repp.aquaticremineztools.domain.compressor.ImageImportManager
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ImageCompressorActivity : AppCompatActivity() {

    private val viewModel: ImageCompressorViewModel by viewModels()

    // Views
    private lateinit var btnBack: FrameLayout
    private lateinit var tvSessionStatus: TextView
    private lateinit var tvBatchCountSize: TextView
    private lateinit var btnImportGallery: Button
    private lateinit var btnImportFiles: Button
    private lateinit var btnImportCamera: Button
    private lateinit var tvEmptySelected: TextView
    private lateinit var rvSelectedImages: RecyclerView

    private lateinit var chipGroupMode: ChipGroup
    private lateinit var chipModeAuto: Chip
    private lateinit var chipModeQuality: Chip
    private lateinit var chipModeTarget: Chip
    private lateinit var chipModePreset: Chip

    private lateinit var layoutQualitySlider: LinearLayout
    private lateinit var tvQualityValue: TextView
    private lateinit var seekBarQuality: SeekBar

    private lateinit var layoutTargetSize: LinearLayout
    private lateinit var spinnerTargetSize: Spinner

    private lateinit var layoutPresets: LinearLayout
    private lateinit var spinnerPreset: Spinner

    private lateinit var spinnerOutputFormat: Spinner

    private lateinit var btnToggleAdvanced: TextView
    private lateinit var layoutAdvancedSettings: LinearLayout
    private lateinit var cbEnableResize: CheckBox
    private lateinit var layoutResizeOptions: LinearLayout
    private lateinit var seekBarResizePercent: SeekBar
    private lateinit var tvResizePercentValue: TextView
    private lateinit var cbStripGps: CheckBox
    private lateinit var cbPreserveIcc: CheckBox

    private lateinit var btnPauseResume: TextView
    private lateinit var btnCancelQueue: TextView
    private lateinit var btnClearQueue: TextView
    private lateinit var tvEmptyQueue: TextView
    private lateinit var rvQueue: RecyclerView

    private lateinit var btnStartCompression: Button

    private lateinit var cardResultsSection: MaterialCardView
    private lateinit var tvBatchSummaryText: TextView
    private lateinit var rvResults: RecyclerView
    private lateinit var btnSaveAll: Button
    private lateinit var btnShareAll: Button
    private lateinit var btnClearResults: Button

    // Adapters
    private lateinit var selectedAdapter: SelectedImageAdapter
    private lateinit var queueAdapter: QueueAdapter
    private lateinit var resultAdapter: ResultAdapter

    // Camera Capture Temporary URI
    private var tempCameraUri: Uri? = null
    private var tempCameraFile: File? = null

    // Photo Picker
    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ImageImportManager.MAX_FILES_PER_BATCH)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    // Storage Access Framework Document Picker
    private val openMultipleDocuments = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.importUris(uris)
        }
    }

    // Camera Launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = tempCameraFile
        val uri = tempCameraUri
        if (success && file != null && uri != null && file.exists() && file.length() > 0) {
            showCameraPreviewDialog(uri)
        } else {
            file?.delete()
            Toast.makeText(this, "Camera capture cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    // Notification Permission Launcher
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_image_compressor)

        checkNotificationPermission()
        bindViews()
        setupAdapters()
        setupListeners()
        setupSpinners()
        observeViewModel()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvSessionStatus = findViewById(R.id.tvSessionStatus)
        tvBatchCountSize = findViewById(R.id.tvBatchCountSize)
        btnImportGallery = findViewById(R.id.btnImportGallery)
        btnImportFiles = findViewById(R.id.btnImportFiles)
        btnImportCamera = findViewById(R.id.btnImportCamera)
        tvEmptySelected = findViewById(R.id.tvEmptySelected)
        rvSelectedImages = findViewById(R.id.rvSelectedImages)

        chipGroupMode = findViewById(R.id.chipGroupMode)
        chipModeAuto = findViewById(R.id.chipModeAuto)
        chipModeQuality = findViewById(R.id.chipModeQuality)
        chipModeTarget = findViewById(R.id.chipModeTarget)
        chipModePreset = findViewById(R.id.chipModePreset)

        layoutQualitySlider = findViewById(R.id.layoutQualitySlider)
        tvQualityValue = findViewById(R.id.tvQualityValue)
        seekBarQuality = findViewById(R.id.seekBarQuality)

        layoutTargetSize = findViewById(R.id.layoutTargetSize)
        spinnerTargetSize = findViewById(R.id.spinnerTargetSize)

        layoutPresets = findViewById(R.id.layoutPresets)
        spinnerPreset = findViewById(R.id.spinnerPreset)

        spinnerOutputFormat = findViewById(R.id.spinnerOutputFormat)

        btnToggleAdvanced = findViewById(R.id.btnToggleAdvanced)
        layoutAdvancedSettings = findViewById(R.id.layoutAdvancedSettings)
        cbEnableResize = findViewById(R.id.cbEnableResize)
        layoutResizeOptions = findViewById(R.id.layoutResizeOptions)
        seekBarResizePercent = findViewById(R.id.seekBarResizePercent)
        tvResizePercentValue = findViewById(R.id.tvResizePercentValue)
        cbStripGps = findViewById(R.id.cbStripGps)
        cbPreserveIcc = findViewById(R.id.cbPreserveIcc)

        btnPauseResume = findViewById(R.id.btnPauseResume)
        btnCancelQueue = findViewById(R.id.btnCancelQueue)
        btnClearQueue = findViewById(R.id.btnClearQueue)
        tvEmptyQueue = findViewById(R.id.tvEmptyQueue)
        rvQueue = findViewById(R.id.rvQueue)

        btnStartCompression = findViewById(R.id.btnStartCompression)

        cardResultsSection = findViewById(R.id.cardResultsSection)
        tvBatchSummaryText = findViewById(R.id.tvBatchSummaryText)
        rvResults = findViewById(R.id.rvResults)
        btnSaveAll = findViewById(R.id.btnSaveAll)
        btnShareAll = findViewById(R.id.btnShareAll)
        btnClearResults = findViewById(R.id.btnClearResults)
    }

    private fun setupAdapters() {
        selectedAdapter = SelectedImageAdapter(
            items = emptyList(),
            onRemoveClick = { item -> viewModel.removeItem(item.id) },
            onItemClick = { item -> showFullPreviewDialog(item) }
        )
        rvSelectedImages.layoutManager = LinearLayoutManager(this)
        rvSelectedImages.adapter = selectedAdapter

        queueAdapter = QueueAdapter(
            items = emptyList(),
            onCancelClick = { item -> viewModel.cancelTask(item.id) }
        )
        rvQueue.layoutManager = LinearLayoutManager(this)
        rvQueue.adapter = queueAdapter

        resultAdapter = ResultAdapter(
            items = emptyList(),
            onSaveClick = { item -> viewModel.saveIndividual(item.id) },
            onShareClick = { item -> shareSingleImage(item) },
            onDiscardClick = { item -> showDiscardSingleDialog(item) }
        )
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = resultAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnImportGallery.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnImportFiles.setOnClickListener {
            openMultipleDocuments.launch(arrayOf("image/*"))
        }

        btnImportCamera.setOnClickListener {
            launchCamera()
        }

        chipGroupMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipModeAuto -> {
                    layoutQualitySlider.visibility = View.GONE
                    layoutTargetSize.visibility = View.GONE
                    layoutPresets.visibility = View.GONE
                    updateConfigFromUi(mode = CompressionMode.AUTO)
                }
                R.id.chipModeQuality -> {
                    layoutQualitySlider.visibility = View.VISIBLE
                    layoutTargetSize.visibility = View.GONE
                    layoutPresets.visibility = View.GONE
                    updateConfigFromUi(mode = CompressionMode.QUALITY)
                }
                R.id.chipModeTarget -> {
                    layoutQualitySlider.visibility = View.GONE
                    layoutTargetSize.visibility = View.VISIBLE
                    layoutPresets.visibility = View.GONE
                    updateConfigFromUi(mode = CompressionMode.TARGET_SIZE)
                }
                R.id.chipModePreset -> {
                    layoutQualitySlider.visibility = View.GONE
                    layoutTargetSize.visibility = View.GONE
                    layoutPresets.visibility = View.VISIBLE
                    updateConfigFromUi(mode = CompressionMode.PRESET)
                }
            }
        }

        seekBarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valPct = progress + 10
                tvQualityValue.text = "$valPct%"
                if (fromUser) {
                    updateConfigFromUi()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnToggleAdvanced.setOnClickListener {
            if (layoutAdvancedSettings.visibility == View.VISIBLE) {
                layoutAdvancedSettings.visibility = View.GONE
                btnToggleAdvanced.text = "▶ Advanced Settings (Resize & Metadata)"
            } else {
                layoutAdvancedSettings.visibility = View.VISIBLE
                btnToggleAdvanced.text = "▼ Advanced Settings (Resize & Metadata)"
            }
        }

        cbEnableResize.setOnCheckedChangeListener { _, isChecked ->
            layoutResizeOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateConfigFromUi()
        }

        seekBarResizePercent.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pct = progress + 10
                tvResizePercentValue.text = "Resize: $pct%"
                if (fromUser) {
                    updateConfigFromUi()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        cbStripGps.setOnCheckedChangeListener { _, isChecked ->
            CompressorPreferences.setStripGps(this, isChecked)
            updateConfigFromUi()
        }

        cbPreserveIcc.setOnCheckedChangeListener { _, isChecked ->
            CompressorPreferences.setPreserveIcc(this, isChecked)
            updateConfigFromUi()
        }

        btnStartCompression.setOnClickListener {
            checkConsentAndStart()
        }

        btnPauseResume.setOnClickListener {
            if (viewModel.isPausedFlow.value) {
                viewModel.resumeQueue()
            } else {
                viewModel.pauseQueue()
            }
        }

        btnCancelQueue.setOnClickListener {
            viewModel.cancelEntireQueue()
        }

        btnClearQueue.setOnClickListener {
            viewModel.clearQueue()
        }

        btnSaveAll.setOnClickListener {
            viewModel.saveAllResults()
        }

        btnShareAll.setOnClickListener {
            shareAllResults()
        }

        btnClearResults.setOnClickListener {
            showDiscardAllDialog()
        }
    }

    private fun setupSpinners() {
        val targetOptions = arrayOf("100 KB", "250 KB", "500 KB", "1 MB", "2 MB", "5 MB", "10 MB")
        val targetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, targetOptions)
        spinnerTargetSize.adapter = targetAdapter

        val presetOptions = CompressionPreset.values().map { it.displayName }
        val presetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, presetOptions)
        spinnerPreset.adapter = presetAdapter

        val formatOptions = OutputFormat.values().map { it.displayName }
        val formatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, formatOptions)
        spinnerOutputFormat.adapter = formatAdapter

        spinnerTargetSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateConfigFromUi()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerPreset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateConfigFromUi()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerOutputFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateConfigFromUi()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateConfigFromUi(mode: CompressionMode? = null) {
        val currentMode = mode ?: when (chipGroupMode.checkedChipId) {
            R.id.chipModeQuality -> CompressionMode.QUALITY
            R.id.chipModeTarget -> CompressionMode.TARGET_SIZE
            R.id.chipModePreset -> CompressionMode.PRESET
            else -> CompressionMode.AUTO
        }

        val qualityVal = seekBarQuality.progress + 10

        val targetSizeBytes = when (spinnerTargetSize.selectedItemPosition) {
            0 -> 100 * 1024L
            1 -> 250 * 1024L
            2 -> 500 * 1024L
            3 -> 1024 * 1024L
            4 -> 2 * 1024 * 1024L
            5 -> 5 * 1024 * 1024L
            6 -> 10 * 1024 * 1024L
            else -> 500 * 1024L
        }

        val presetEnum = CompressionPreset.values().getOrElse(spinnerPreset.selectedItemPosition) { CompressionPreset.MEDIUM }
        val formatEnum = OutputFormat.values().getOrElse(spinnerOutputFormat.selectedItemPosition) { OutputFormat.ORIGINAL }

        val isResize = cbEnableResize.isChecked
        val resizePct = seekBarResizePercent.progress + 10

        val config = CompressionConfig(
            mode = currentMode,
            quality = qualityVal,
            targetSizeBytes = targetSizeBytes,
            preset = presetEnum,
            outputFormat = formatEnum,
            enableResize = isResize,
            resizeMode = if (isResize) ResizeMode.PERCENTAGE else ResizeMode.NONE,
            resizePercent = resizePct,
            stripGps = cbStripGps.isChecked,
            preserveIcc = cbPreserveIcc.isChecked
        )

        viewModel.updateConfig(config)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.toastEvent.collect { msg ->
                Toast.makeText(this@ImageCompressorActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.itemsFlow.collectLatest { items ->
                val readyItems = items.filter { it.status == ItemStatus.READY || it.status == ItemStatus.CHECKING }
                val queuedItems = items.filter {
                    it.status in listOf(
                        ItemStatus.QUEUED,
                        ItemStatus.UPLOADING,
                        ItemStatus.PROCESSING,
                        ItemStatus.DOWNLOADING,
                        ItemStatus.VALIDATING
                    )
                }
                val resultItems = items.filter {
                    it.status == ItemStatus.COMPLETED || it.status == ItemStatus.SAVED || it.status == ItemStatus.FAILED
                }

                // Section 1: Selected Images
                selectedAdapter.updateData(readyItems)
                if (readyItems.isEmpty()) {
                    tvEmptySelected.visibility = View.VISIBLE
                    rvSelectedImages.visibility = View.GONE
                } else {
                    tvEmptySelected.visibility = View.GONE
                    rvSelectedImages.visibility = View.VISIBLE
                }

                val totalSizeMb = readyItems.sumOf { it.fileSize } / (1024.0 * 1024.0)
                tvBatchCountSize.text = "${readyItems.size} / 10 images (${String.format("%.1f", totalSizeMb)} MB / 200 MB)"

                // Section 3: Queue
                queueAdapter.updateData(queuedItems)
                if (queuedItems.isEmpty()) {
                    tvEmptyQueue.visibility = View.VISIBLE
                    rvQueue.visibility = View.GONE
                } else {
                    tvEmptyQueue.visibility = View.GONE
                    rvQueue.visibility = View.VISIBLE
                }

                // Section 5: Results
                resultAdapter.updateData(resultItems)
                if (resultItems.isEmpty()) {
                    cardResultsSection.visibility = View.GONE
                } else {
                    cardResultsSection.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isProcessingFlow.collectLatest { isProc ->
                btnPauseResume.visibility = if (isProc) View.VISIBLE else View.GONE
                btnCancelQueue.visibility = if (isProc) View.VISIBLE else View.GONE
                tvSessionStatus.text = if (isProc) "PROCESSING" else "IDLE"
                tvSessionStatus.setTextColor(if (isProc) 0xFF38BFA1.toInt() else 0xFF43D9E6.toInt())
            }
        }

        lifecycleScope.launch {
            viewModel.isPausedFlow.collectLatest { isPaused ->
                if (isPaused) {
                    btnPauseResume.text = "Resume"
                    tvSessionStatus.text = "PAUSED"
                    tvSessionStatus.setTextColor(0xFFD6B46A.toInt())
                } else {
                    btnPauseResume.text = "Pause"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.batchSummaryFlow.collectLatest { summary ->
                if (summary.totalCount > 0) {
                    val savedMb = summary.totalSavedBytes / (1024.0 * 1024.0)
                    tvBatchSummaryText.text = String.format("%d Completed • %.2f MB Saved (%.1f%%)", summary.successCount, savedMb, summary.savingPercentage)
                }
            }
        }
    }

    private fun checkConsentAndStart() {
        val consent = CompressorPreferences.getCloudConsent(this)
        if (consent == CloudConsentState.NOT_SET) {
            showCloudConsentDialog()
        } else {
            viewModel.startCompression()
        }
    }

    private fun showCloudConsentDialog() {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Third-Party Processing Consent")
            .setMessage("Image Compressor supports cloud-assisted optimization through managed proxy services.\n\n" +
                    "• Your image is transmitted securely over HTTPS.\n" +
                    "• Location metadata (GPS) is stripped locally prior to upload.\n" +
                    "• No account registration or tracking is stored.\n\n" +
                    "Do you consent to enable third-party cloud processing?")
            .setPositiveButton("I Agree") { _, _ ->
                CompressorPreferences.setCloudConsent(this, CloudConsentState.GRANTED)
                viewModel.startCompression()
            }
            .setNegativeButton("Local Only") { _, _ ->
                CompressorPreferences.setCloudConsent(this, CloudConsentState.DENIED)
                viewModel.startCompression()
            }
            .setCancelable(false)
            .show()
    }

    private fun launchCamera() {
        try {
            val tempDir = ImageImportManager.getTempDir(this)
            val file = File(tempDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to launch camera.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCameraPreviewDialog(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_camera_preview, null)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivCameraPreview)
        val btnUsePhoto = dialogView.findViewById<Button>(R.id.btnUsePhoto)
        val btnRetake = dialogView.findViewById<Button>(R.id.btnRetake)

        ivPreview.load(uri)

        val dialog = AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnUsePhoto.setOnClickListener {
            dialog.dismiss()
            viewModel.importUris(listOf(uri))
        }

        btnRetake.setOnClickListener {
            dialog.dismiss()
            tempCameraFile?.delete()
            launchCamera()
        }

        dialog.show()
    }

    private fun showFullPreviewDialog(item: CompressorItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_full_preview, null)
        val ivFull = dialogView.findViewById<ImageView>(R.id.ivFullImage)
        val tvDetails = dialogView.findViewById<TextView>(R.id.tvFullDetails)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseFull)

        ivFull.load(item.uri)
        val sizeMb = String.format("%.2f MB", item.fileSize / (1024.0 * 1024.0))
        tvDetails.text = "${item.fileName}\n$sizeMb • ${item.width}x${item.height} • ${item.mimeType}"

        val dialog = AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun shareSingleImage(item: CompressorItem) {
        val path = item.compressedUri ?: item.uri
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(this, "File missing for sharing.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Compressed Image"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Sharing failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAllResults() {
        val items = viewModel.itemsFlow.value.filter { it.status == ItemStatus.COMPLETED || it.status == ItemStatus.SAVED }
        if (items.isEmpty()) return

        try {
            val uris = ArrayList<Uri>()
            for (item in items) {
                val path = item.compressedUri ?: item.uri
                val file = File(path)
                if (file.exists()) {
                    uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
                }
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share All Compressed Images"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Sharing multiple images failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDiscardSingleDialog(item: CompressorItem) {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Discard Result")
            .setMessage("Are you sure you want to discard ${item.fileName}?")
            .setPositiveButton("Discard") { _, _ ->
                viewModel.discardIndividual(item.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDiscardAllDialog() {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Discard All Results")
            .setMessage("Are you sure you want to discard all unsaved compression results?")
            .setPositiveButton("Discard All") { _, _ ->
                viewModel.discardAllResults()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
