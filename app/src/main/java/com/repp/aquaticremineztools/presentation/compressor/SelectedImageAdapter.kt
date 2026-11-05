package com.repp.aquaticremineztools.presentation.compressor

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import java.io.File

class SelectedImageAdapter(
    private var items: List<CompressorItem>,
    private val onRemoveClick: (CompressorItem) -> Unit,
    private val onItemClick: (CompressorItem) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_ADD_TILE = 2
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View = itemView.findViewById(R.id.cardSelectedImage)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnRemoveFrame: FrameLayout = itemView.findViewById(R.id.btnRemoveFrame)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        val tvResFormat: TextView = itemView.findViewById(R.id.tvResFormat)
        val layoutErrorThumbnail: LinearLayout = itemView.findViewById(R.id.layoutErrorThumbnail)
        val pbRetrySpinner: ProgressBar = itemView.findViewById(R.id.pbRetrySpinner)
        val btnRetryThumbnail: TextView = itemView.findViewById(R.id.btnRetryThumbnail)
        val ivErrorIcon: ImageView = itemView.findViewById(R.id.ivErrorIcon)
        val tvErrorMsg: TextView = itemView.findViewById(R.id.tvErrorMsg)
    }

    class AddTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (position < items.size) VIEW_TYPE_IMAGE else VIEW_TYPE_ADD_TILE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_IMAGE) {
            val view = inflater.inflate(R.layout.item_compressor_selected_image, parent, false)
            ImageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_compressor_add_tile, parent, false)
            AddTileViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ADD_TILE) {
            holder.itemView.setOnClickListener { onAddClick() }
            return
        }

        val itemHolder = holder as ImageViewHolder
        val item = items[position]

        itemHolder.tvFileName.text = item.fileName

        val sizeMb = String.format("%.2f MB", item.fileSize / (1024.0 * 1024.0))
        itemHolder.tvFileSize.text = sizeMb

        val formatStr = when {
            item.mimeType.contains("jpeg", ignoreCase = true) || item.mimeType.contains("jpg", ignoreCase = true) -> "JPEG"
            item.mimeType.contains("png", ignoreCase = true) -> "PNG"
            item.mimeType.contains("webp", ignoreCase = true) -> "WebP"
            item.mimeType.contains("gif", ignoreCase = true) -> "GIF"
            item.mimeType.contains("avif", ignoreCase = true) -> "AVIF"
            item.mimeType.contains("heic", ignoreCase = true) -> "HEIC"
            else -> item.mimeType.substringAfter('/').uppercase()
        }
        val resStr = if (item.width > 0 && item.height > 0) "${item.width} × ${item.height}" else "—"
        itemHolder.tvResFormat.text = "$resStr · $formatStr"

        loadThumbnail(itemHolder, item)

        itemHolder.btnRemoveFrame.setOnClickListener {
            onRemoveClick(item)
        }

        itemHolder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    private fun loadThumbnail(holder: ImageViewHolder, item: CompressorItem) {
        holder.layoutErrorThumbnail.visibility = View.GONE
        holder.pbRetrySpinner.visibility = View.GONE

        val source = if (item.uri.startsWith("content://") || item.uri.startsWith("file://")) {
            item.uri
        } else {
            File(item.uri)
        }

        val request = ImageRequest.Builder(holder.itemView.context)
            .data(source)
            .target(
                onSuccess = { drawable ->
                    holder.layoutErrorThumbnail.visibility = View.GONE
                    holder.ivThumbnail.setImageDrawable(drawable)
                },
                onError = {
                    holder.layoutErrorThumbnail.visibility = View.VISIBLE
                    holder.ivErrorIcon.visibility = View.VISIBLE
                    holder.tvErrorMsg.visibility = View.VISIBLE
                    holder.btnRetryThumbnail.visibility = View.VISIBLE
                    holder.pbRetrySpinner.visibility = View.GONE
                }
            )
            .build()

        holder.itemView.context.applicationContext.let { ctx ->
            coil.ImageLoader(ctx).enqueue(request)
        }

        holder.btnRetryThumbnail.setOnClickListener {
            holder.pbRetrySpinner.visibility = View.VISIBLE
            holder.btnRetryThumbnail.visibility = View.GONE
            holder.ivErrorIcon.visibility = View.GONE
            holder.tvErrorMsg.visibility = View.GONE

            loadThumbnail(holder, item)
        }
    }

    override fun getItemCount(): Int {
        if (items.isEmpty()) return 0
        return if (items.size < 10) items.size + 1 else 10
    }

    fun updateData(newItems: List<CompressorItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun pulseHighlightDuplicate(itemId: String) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            notifyItemChanged(index, "HIGHLIGHT")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains("HIGHLIGHT") && holder is ImageViewHolder) {
            runPulseAnimation(holder.cardView)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun runPulseAnimation(view: View) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.08f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.08f, 1.0f)
        val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            duration = 350
        }
        animator.start()
    }
}
