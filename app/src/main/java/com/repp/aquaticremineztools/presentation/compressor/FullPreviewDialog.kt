package com.repp.aquaticremineztools.presentation.compressor

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import java.io.File

class FullPreviewDialog(
    context: Context,
    private val items: List<CompressorItem>,
    private val initialPosition: Int = 0
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var btnBackPreview: View
    private lateinit var viewPagerPreview: ViewPager2
    private lateinit var layoutDots: LinearLayout
    private lateinit var tvPreviewFileName: TextView
    private lateinit var tvPreviewResolution: TextView
    private lateinit var tvPreviewFormat: TextView
    private lateinit var tvPreviewFileSize: TextView
    private lateinit var btnToggleDetail: View
    private lateinit var ivChevronDetail: ImageView
    private lateinit var layoutProfessionalDetail: View
    private lateinit var tvDetailMimeType: TextView
    private lateinit var tvDetailAspectRatio: TextView
    private lateinit var tvDetailColorSpace: TextView
    private lateinit var tvDetailPixelCount: TextView

    private var isDetailExpanded = false
    private val imageViewsMap = mutableMapOf<Int, ZoomableImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_full_preview)

        window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            w.statusBarColor = Color.TRANSPARENT
            w.navigationBarColor = Color.TRANSPARENT
        }

        bindViews()
        setupWindowInsets()
        setupViewPager()
        setupDots()
        setupListeners()

        val pos = initialPosition.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (items.isNotEmpty()) {
            viewPagerPreview.setCurrentItem(pos, false)
            updatePageContent(pos)
        }
    }

    private fun bindViews() {
        btnBackPreview = findViewById(R.id.btnBackPreview)
        viewPagerPreview = findViewById(R.id.viewPagerPreview)
        layoutDots = findViewById(R.id.layoutDots)
        tvPreviewFileName = findViewById(R.id.tvPreviewFileName)
        tvPreviewResolution = findViewById(R.id.tvPreviewResolution)
        tvPreviewFormat = findViewById(R.id.tvPreviewFormat)
        tvPreviewFileSize = findViewById(R.id.tvPreviewFileSize)
        btnToggleDetail = findViewById(R.id.btnToggleDetail)
        ivChevronDetail = findViewById(R.id.ivChevronDetail)
        layoutProfessionalDetail = findViewById(R.id.layoutProfessionalDetail)
        tvDetailMimeType = findViewById(R.id.tvDetailMimeType)
        tvDetailAspectRatio = findViewById(R.id.tvDetailAspectRatio)
        tvDetailColorSpace = findViewById(R.id.tvDetailColorSpace)
        tvDetailPixelCount = findViewById(R.id.tvDetailPixelCount)
    }

    private fun setupWindowInsets() {
        val root = findViewById<View>(R.id.previewRootLayout)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val topBar = findViewById<View>(R.id.topBarPreview)
            topBar.setPadding(
                topBar.paddingLeft,
                statusBarHeight + 12,
                topBar.paddingRight,
                topBar.paddingBottom
            )

            val scrollDetail = findViewById<View>(R.id.scrollDetailArea)
            scrollDetail.setPadding(
                scrollDetail.paddingLeft,
                scrollDetail.paddingTop,
                scrollDetail.paddingRight,
                navBarHeight + 16
            )
            insets
        }
    }

    private fun setupViewPager() {
        val adapter = PreviewPagerAdapter()
        viewPagerPreview.adapter = adapter

        viewPagerPreview.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Reset zoom on all views
                imageViewsMap.values.forEach { it.resetZoom() }

                // Reset expandable detail section
                if (isDetailExpanded) {
                    collapseDetailSection(animate = false)
                }

                updateDots(position)
                updatePageContent(position)
            }
        })
    }

    private fun setupDots() {
        layoutDots.removeAllViews()
        if (items.size <= 0) return

        for (i in items.indices) {
            val dot = View(context).apply {
                val params = LinearLayout.LayoutParams(
                    if (i == initialPosition) 10.toPx() else 8.toPx(),
                    if (i == initialPosition) 10.toPx() else 8.toPx()
                ).apply {
                    setMargins(5.toPx(), 0, 5.toPx(), 0)
                }
                layoutParams = params
                setBackgroundResource(
                    if (i == initialPosition) R.drawable.bg_dot_active_preview else R.drawable.bg_dot_inactive_preview
                )
            }
            layoutDots.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        for (i in 0 until layoutDots.childCount) {
            val dot = layoutDots.getChildAt(i)
            val params = dot.layoutParams as LinearLayout.LayoutParams
            if (i == position) {
                params.width = 10.toPx()
                params.height = 10.toPx()
                dot.setBackgroundResource(R.drawable.bg_dot_active_preview)
            } else {
                params.width = 8.toPx()
                params.height = 8.toPx()
                dot.setBackgroundResource(R.drawable.bg_dot_inactive_preview)
            }
            dot.layoutParams = params
        }
    }

    private fun updatePageContent(position: Int) {
        if (position !in items.indices) return
        val item = items[position]

        tvPreviewFileName.text = item.fileName

        val resText = if (item.width > 0 && item.height > 0) "${item.width} × ${item.height}" else "—"
        tvPreviewResolution.text = "Resolution: $resText"

        val formatText = when {
            item.mimeType.contains("jpeg", ignoreCase = true) || item.mimeType.contains("jpg", ignoreCase = true) -> "JPEG"
            item.mimeType.contains("png", ignoreCase = true) -> "PNG"
            item.mimeType.contains("webp", ignoreCase = true) -> "WebP"
            item.mimeType.contains("gif", ignoreCase = true) -> "GIF"
            item.mimeType.contains("avif", ignoreCase = true) -> "AVIF"
            item.mimeType.contains("heic", ignoreCase = true) -> "HEIC"
            else -> item.mimeType.substringAfter('/').uppercase()
        }
        tvPreviewFormat.text = "Format: $formatText"

        val sizeMbText = String.format("%.2f MB", item.fileSize / (1024.0 * 1024.0))
        tvPreviewFileSize.text = "Size: $sizeMbText"

        // Professional details
        tvDetailMimeType.text = if (item.mimeType.isNotBlank()) item.mimeType else "—"
        tvDetailAspectRatio.text = calculateAspectRatio(item.width, item.height)
        tvDetailColorSpace.text = "sRGB"
        tvDetailPixelCount.text = calculatePixelCount(item.width, item.height)
    }

    private fun calculateAspectRatio(w: Int, h: Int): String {
        if (w <= 0 || h <= 0) return "—"
        val gcd = gcd(w, h)
        val rw = w / gcd
        val rh = h / gcd
        return "$rw:$rh"
    }

    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    private fun calculatePixelCount(w: Int, h: Int): String {
        if (w <= 0 || h <= 0) return "—"
        val mp = (w.toDouble() * h.toDouble()) / 1_000_000.0
        return if (mp >= 1.0) {
            String.format("%.1f MP", mp)
        } else {
            String.format("%.2f MP", mp)
        }
    }

    private fun setupListeners() {
        btnBackPreview.setOnClickListener { dismiss() }

        btnToggleDetail.setOnClickListener {
            if (isDetailExpanded) {
                collapseDetailSection(animate = true)
            } else {
                expandDetailSection(animate = true)
            }
        }
    }

    private fun expandDetailSection(animate: Boolean) {
        isDetailExpanded = true
        layoutProfessionalDetail.visibility = View.VISIBLE
        ivChevronDetail.animate().rotation(180f).setDuration(if (animate) 200L else 0L).start()
    }

    private fun collapseDetailSection(animate: Boolean) {
        isDetailExpanded = false
        layoutProfessionalDetail.visibility = View.GONE
        ivChevronDetail.animate().rotation(0f).setDuration(if (animate) 200L else 0L).start()
    }

    private inner class PreviewPagerAdapter : RecyclerView.Adapter<PreviewViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
            val zoomView = ZoomableImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            return PreviewViewHolder(zoomView)
        }

        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
            val item = items[position]
            val zoomView = holder.zoomView
            imageViewsMap[position] = zoomView

            val source = if (item.uri.startsWith("content://") || item.uri.startsWith("file://")) {
                item.uri
            } else {
                File(item.uri)
            }
            zoomView.load(source) {
                listener(onSuccess = { _, _ -> zoomView.resetZoom() })
            }
        }

        override fun getItemCount(): Int = items.size

        override fun onViewRecycled(holder: PreviewViewHolder) {
            holder.zoomView.resetZoom()
            super.onViewRecycled(holder)
        }
    }

    private class PreviewViewHolder(val zoomView: ZoomableImageView) : RecyclerView.ViewHolder(zoomView)

    private fun Int.toPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
