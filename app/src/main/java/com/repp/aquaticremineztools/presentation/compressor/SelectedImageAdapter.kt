package com.repp.aquaticremineztools.presentation.compressor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import java.io.File

class SelectedImageAdapter(
    private var items: List<CompressorItem>,
    private val onRemoveClick: (CompressorItem) -> Unit,
    private val onItemClick: (CompressorItem) -> Unit
) : RecyclerView.Adapter<SelectedImageAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnRemove: ImageView = itemView.findViewById(R.id.btnRemove)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compressor_selected_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvFileName.text = item.fileName
        val sizeMb = String.format("%.2f MB", item.fileSize / (1024.0 * 1024.0))
        holder.tvDetails.text = "$sizeMb • ${item.width}x${item.height} • ${item.mimeType.substringAfter('/')}"
        holder.tvStatus.text = item.status.name

        if (item.uri.startsWith("content://") || item.uri.startsWith("file://")) {
            holder.ivThumbnail.load(item.uri)
        } else {
            holder.ivThumbnail.load(File(item.uri))
        }

        holder.btnRemove.setOnClickListener {
            onRemoveClick(item)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CompressorItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
