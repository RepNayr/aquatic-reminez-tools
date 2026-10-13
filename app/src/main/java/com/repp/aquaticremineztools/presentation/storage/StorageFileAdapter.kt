package com.repp.aquaticremineztools.presentation.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R

class StorageFileAdapter(
    private val files: List<StorageFile>,
    private val onItemClick: (StorageFile) -> Unit,
    private val onItemLongClick: (StorageFile) -> Unit,
    private val onItemSelect: (StorageFile, Boolean) -> Unit,
) : RecyclerView.Adapter<StorageFileAdapter.FileViewHolder>() {

    private val selectedItems = mutableSetOf<String>()
    private var selectionMode = false

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: View = itemView.findViewById(R.id.cardFileItem)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivFileThumbnail)
        val ivTypeIcon: ImageView = itemView.findViewById(R.id.ivFileTypeIcon)
        val selectedOverlay: View = itemView.findViewById(R.id.selectedOverlay)
        val ivSelectedCheck: ImageView = itemView.findViewById(R.id.ivSelectedCheck)
        val tvName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvSize: TextView = itemView.findViewById(R.id.tvFileSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_storage_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = files[position]
        val context = holder.itemView.context

        holder.tvName.text = item.name
        holder.tvSize.text = StorageHelper.formatSize(item.size)

        // Thumbnail dinamis berdasarkan tipe
        when (item.type) {
            StorageFile.FileType.IMAGE -> {
                holder.ivThumbnail.visibility = View.VISIBLE
                holder.ivTypeIcon.visibility = View.GONE
                holder.ivThumbnail.load(item.file)
            }
            StorageFile.FileType.VIDEO -> {
                holder.ivThumbnail.visibility = View.VISIBLE
                holder.ivTypeIcon.visibility = View.VISIBLE
                holder.ivTypeIcon.setImageResource(R.drawable.ic_file_video)
                holder.ivThumbnail.load(item.file)
            }
            StorageFile.FileType.AUDIO -> {
                holder.ivThumbnail.visibility = View.VISIBLE
                holder.ivTypeIcon.visibility = View.VISIBLE
                holder.ivTypeIcon.setImageResource(R.drawable.ic_file_audio)
                holder.ivThumbnail.setImageResource(R.drawable.ic_file_audio)
            }
            StorageFile.FileType.DOCUMENT -> {
                holder.ivThumbnail.visibility = View.VISIBLE
                holder.ivTypeIcon.visibility = View.VISIBLE
                holder.ivTypeIcon.setImageResource(R.drawable.ic_file_doc)
                holder.ivThumbnail.setImageResource(R.drawable.ic_file_doc)
            }
            StorageFile.FileType.OTHER -> {
                holder.ivThumbnail.visibility = View.VISIBLE
                holder.ivTypeIcon.visibility = View.VISIBLE
                holder.ivTypeIcon.setImageResource(R.drawable.ic_file_other)
                holder.ivThumbnail.setImageResource(R.drawable.ic_file_other)
            }
        }

        // Selection state
        val isSelected = selectedItems.contains(item.path)
        holder.selectedOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.ivSelectedCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Click
        holder.cardRoot.setOnClickListener {
            if (selectionMode) {
                toggleSelection(item)
            } else {
                onItemClick(item)
            }
        }

        // Long click
        holder.cardRoot.setOnLongClickListener {
            selectionMode = true
            toggleSelection(item)
            true
        }
    }

    private fun toggleSelection(item: StorageFile) {
        val path = item.path
        if (selectedItems.contains(path)) {
            selectedItems.remove(path)
            onItemSelect(item, false)
        } else {
            selectedItems.add(path)
            onItemSelect(item, true)
        }

        if (selectedItems.isEmpty()) {
            selectionMode = false
        }

        notifyDataSetChanged()
    }

    fun getSelectedFiles(): List<StorageFile> {
        return files.filter { selectedItems.contains(it.path) }
    }

    fun clearSelection() {
        selectedItems.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun isSelectionMode(): Boolean = selectionMode

    override fun getItemCount(): Int = files.size
}