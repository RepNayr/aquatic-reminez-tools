package com.repp.aquaticremineztools.presentation.compressor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.compressor.CompressorItem
import java.io.File

class QueueAdapter(
    private var items: List<CompressorItem>,
    private val onCancelClick: (CompressorItem) -> Unit
) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val btnCancel: ImageView = itemView.findViewById(R.id.btnCancel)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvQueueInfo: TextView = itemView.findViewById(R.id.tvQueueInfo)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compressor_queue, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvFileName.text = item.fileName
        holder.tvQueueInfo.text = "Position #${item.queuePosition + 1} • Status: ${item.status.name}"
        holder.progressBar.progress = item.progress

        if (item.uri.startsWith("content://") || item.uri.startsWith("file://")) {
            holder.ivThumbnail.load(item.uri)
        } else {
            holder.ivThumbnail.load(File(item.uri))
        }

        holder.btnCancel.setOnClickListener {
            onCancelClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CompressorItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
