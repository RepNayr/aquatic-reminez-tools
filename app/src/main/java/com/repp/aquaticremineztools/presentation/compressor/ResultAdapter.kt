package com.repp.aquaticremineztools.presentation.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import com.repp.aquaticremineztools.data.compressor.ItemStatus
import com.repp.aquaticremineztools.domain.compressor.ImageMetadataManager
import java.io.File
import java.util.Locale

class ResultAdapter(
    private var items: List<CompressorItem>,
    private val onSaveClick: (CompressorItem) -> Unit,
    private val onShareClick: (CompressorItem) -> Unit,
    private val onDiscardClick: (CompressorItem) -> Unit
) : RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val comparisonView: ImageComparisonView = itemView.findViewById(R.id.comparisonView)
        val tvResultFileName: TextView = itemView.findViewById(R.id.tvResultFileName)
        val tvCategoryBadge: TextView = itemView.findViewById(R.id.tvCategoryBadge)
        val tvSizeCompare: TextView = itemView.findViewById(R.id.tvSizeCompare)
        val tvSavingsBadge: TextView = itemView.findViewById(R.id.tvSavingsBadge)
        val tvMetricsDetails: TextView = itemView.findViewById(R.id.tvMetricsDetails)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val btnSave: Button = itemView.findViewById(R.id.btnSave)
        val btnShare: Button = itemView.findViewById(R.id.btnShare)
        val btnDiscard: Button = itemView.findViewById(R.id.btnDiscard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compressor_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvResultFileName.text = item.fileName
        holder.tvCategoryBadge.text = item.detectedCategory ?: "Photo"

        val origMb = item.originalSize / (1024.0 * 1024.0)
        val compMb = item.compressedSize / (1024.0 * 1024.0)
        val savingsPct = if (item.originalSize > 0) {
            ((item.originalSize - item.compressedSize).toDouble() / item.originalSize.toDouble()) * 100.0
        } else 0.0

        holder.tvSizeCompare.text = String.format(Locale.US, "%.2f MB → %.2f MB", origMb, compMb)

        if (item.isNotBeneficial) {
            holder.tvSavingsBadge.text = "0% (Original Retained)"
            holder.tvSavingsBadge.setTextColor(0xFFD6B46A.toInt())
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = "Compression produced larger file size. Original image retained as optimized result."
        } else {
            holder.tvSavingsBadge.text = String.format(Locale.US, "-%.1f%% Saved", savingsPct.coerceAtLeast(0.0))
            holder.tvSavingsBadge.setTextColor(0xFF38BFA1.toInt())
            holder.tvNote.visibility = View.GONE
        }

        // Detailed Professional Metrics
        val psnrStr = if (item.psnrDb != null && item.psnrDb > 0.0) String.format(Locale.US, "%.1f dB", item.psnrDb) else "N/A"
        val ssimStr = if (item.ssimScore != null && item.ssimScore > 0.0) String.format(Locale.US, "%.2f", item.ssimScore) else "N/A"
        val targetStatusStr = when (item.targetSizeAchieved) {
            true -> "• Target: Achieved ✓"
            false -> "• Target: Missed"
            null -> ""
        }

        val origFmt = item.mimeType.substringAfter("/").uppercase()
        val compFmt = item.compressedFormat?.uppercase() ?: origFmt

        holder.tvMetricsDetails.text = String.format(
            Locale.US,
            "Res: %dx%d → %dx%d • Format: %s → %s\nPSNR: %s • SSIM: %s • Time: %d ms %s",
            item.width, item.height,
            item.compressedWidth.let { if (it > 0) it else item.width },
            item.compressedHeight.let { if (it > 0) it else item.height },
            origFmt, compFmt,
            psnrStr, ssimStr,
            item.processingTimeMs,
            targetStatusStr
        )

        if (item.status == ItemStatus.SAVED) {
            holder.btnSave.isEnabled = false
            holder.btnSave.text = "Saved ✓"
        } else {
            holder.btnSave.isEnabled = true
            holder.btnSave.text = "Save to Gallery"
        }

        // Downsampled Bitmap Loading for RAM Safety (Max 512x512 preview)
        try {
            val beforePath = item.localTempPath ?: item.uri
            val beforeBitmap = decodeFileSampled(context, beforePath, 512, 512)
            val afterPath = item.compressedUri ?: beforePath
            val afterBitmap = decodeFileSampled(context, afterPath, 512, 512)
            holder.comparisonView.setBitmaps(beforeBitmap, afterBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        holder.btnSave.setOnClickListener { onSaveClick(item) }
        holder.btnShare.setOnClickListener { onShareClick(item) }
        holder.btnDiscard.setOnClickListener { onDiscardClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CompressorItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun decodeFileSampled(context: Context, path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } else {
                BitmapFactory.decodeFile(path, options)
            }

            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val bitmap = if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } else {
                BitmapFactory.decodeFile(path, options)
            }

            if (bitmap != null) {
                val file = File(path)
                if (file.exists()) {
                    val orientation = ImageMetadataManager.getExifOrientation(file)
                    ImageMetadataManager.applyExifOrientation(bitmap, orientation)
                } else {
                    bitmap
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
