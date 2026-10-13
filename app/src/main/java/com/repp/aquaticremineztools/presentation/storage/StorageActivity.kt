package com.repp.aquaticremineztools.presentation.storage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import java.io.File

class StorageActivity : AppCompatActivity() {

    private lateinit var rvStorage: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var btnSort: FrameLayout

    private lateinit var tabAll: TextView
    private lateinit var tabImage: TextView
    private lateinit var tabVideo: TextView
    private lateinit var tabAudio: TextView
    private lateinit var tabDoc: TextView

    private var allFiles: List<StorageFile> = emptyList()
    private var displayedFiles: List<StorageFile> = emptyList()
    private var currentFilter = FilterType.ALL
    private lateinit var adapter: StorageFileAdapter

    private enum class FilterType { ALL, IMAGE, VIDEO, AUDIO, DOC }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage)

        bindViews()
        setupTabs()
        setupRecyclerView()
        setupClicks()
        loadFiles()
    }

    private fun bindViews() {
        rvStorage = findViewById(R.id.rvStorage)
        emptyState = findViewById(R.id.emptyState)
        btnBack = findViewById(R.id.btnBackStorage)
        btnSort = findViewById(R.id.btnSortStorage)

        tabAll = findViewById(R.id.tabAll)
        tabImage = findViewById(R.id.tabImage)
        tabVideo = findViewById(R.id.tabVideo)
        tabAudio = findViewById(R.id.tabAudio)
        tabDoc = findViewById(R.id.tabDoc)
    }

    private fun setupClicks() {
        btnBack.setOnClickListener { finish() }
        btnSort.setOnClickListener { showSortDialog() }
    }

    private fun setupTabs() {
        tabAll.setOnClickListener { setFilter(FilterType.ALL) }
        tabImage.setOnClickListener { setFilter(FilterType.IMAGE) }
        tabVideo.setOnClickListener { setFilter(FilterType.VIDEO) }
        tabAudio.setOnClickListener { setFilter(FilterType.AUDIO) }
        tabDoc.setOnClickListener { setFilter(FilterType.DOC) }

        updateTabStyles()
    }

    private fun setFilter(filter: FilterType) {
        currentFilter = filter
        updateTabStyles()
        applyFilter()
    }

    private fun updateTabStyles() {
        val activeBg = R.drawable.bg_storage_tab_active
        val inactiveBg = R.drawable.bg_storage_tab_inactive

        tabAll.setBackgroundResource(if (currentFilter == FilterType.ALL) activeBg else inactiveBg)
        tabImage.setBackgroundResource(if (currentFilter == FilterType.IMAGE) activeBg else inactiveBg)
        tabVideo.setBackgroundResource(if (currentFilter == FilterType.VIDEO) activeBg else inactiveBg)
        tabAudio.setBackgroundResource(if (currentFilter == FilterType.AUDIO) activeBg else inactiveBg)
        tabDoc.setBackgroundResource(if (currentFilter == FilterType.DOC) activeBg else inactiveBg)
    }

    private fun applyFilter() {
        displayedFiles = when (currentFilter) {
            FilterType.ALL -> allFiles
            FilterType.IMAGE -> allFiles.filter { it.type == StorageFile.FileType.IMAGE }
            FilterType.VIDEO -> allFiles.filter { it.type == StorageFile.FileType.VIDEO }
            FilterType.AUDIO -> allFiles.filter { it.type == StorageFile.FileType.AUDIO }
            FilterType.DOC -> allFiles.filter { it.type == StorageFile.FileType.DOCUMENT }
        }
        updateList()
    }

    private fun setupRecyclerView() {
        adapter = StorageFileAdapter(
            files = displayedFiles,
            onItemClick = { file -> openFile(file) },
            onItemLongClick = { file -> showFileMenu(file) },
            onItemSelect = { _, _ -> /* Bisa dikembangin nanti */ },
        )
        rvStorage.layoutManager = GridLayoutManager(this, 2)
        rvStorage.adapter = adapter
    }

    private fun loadFiles() {
        allFiles = StorageHelper.listFiles(this)
        applyFilter()
    }

    private fun updateList() {
        if (displayedFiles.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvStorage.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvStorage.visibility = View.VISIBLE
        }

        setupRecyclerView()
    }

    private fun openFile(file: StorageFile) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file.file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak ada app untuk buka file ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileMenu(file: StorageFile) {
        val options = arrayOf(
            getString(R.string.storage_action_open),
            getString(R.string.storage_action_share),
            getString(R.string.storage_action_rename),
            getString(R.string.storage_action_info),
            getString(R.string.storage_action_delete),
        )

        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFile(file)
                    1 -> shareFile(file)
                    2 -> showRenameDialog(file)
                    3 -> showFileInfo(file)
                    4 -> confirmDelete(file)
                }
            }
            .show()
    }

    private fun shareFile(file: StorageFile) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file.file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share"))
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal share file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameDialog(file: StorageFile) {
        val input = android.widget.EditText(this).apply {
            setText(file.name.substringBeforeLast('.'))
            setHint(R.string.storage_rename_hint)
            setSelection(text.length)
        }

        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(R.string.storage_rename_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newBaseName = input.text.toString().trim()
                if (newBaseName.isEmpty()) {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val ext = file.name.substringAfterLast('.', "")
                val newName = if (ext.isEmpty()) newBaseName else "$newBaseName.$ext"

                val newFile = StorageHelper.renameFile(file.file, newName)
                if (newFile != null) {
                    Toast.makeText(this, R.string.storage_renamed, Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this, "Gagal rename", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFileInfo(file: StorageFile) {
        val info = """
            ${getString(R.string.storage_action_info)}
            
            Name: ${file.name}
            Size: ${StorageHelper.formatSize(file.size)}
            Type: ${file.type.name}
            Modified: ${StorageHelper.formatDate(file.lastModified)}
            Path: ${file.path}
        """.trimIndent()

        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(file.name)
            .setMessage(info)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun confirmDelete(file: StorageFile) {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(R.string.storage_action_delete)
            .setMessage(R.string.storage_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (StorageHelper.deleteFile(file.file)) {
                    Toast.makeText(this, R.string.storage_deleted, Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this, "Gagal hapus file", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Name A-Z", "Name Z-A", "Newest", "Oldest", "Largest", "Smallest")
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(R.string.cat_sort)
            .setItems(options) { _, which ->
                allFiles = when (which) {
                    0 -> allFiles.sortedBy { it.name.lowercase() }
                    1 -> allFiles.sortedByDescending { it.name.lowercase() }
                    2 -> allFiles.sortedByDescending { it.lastModified }
                    3 -> allFiles.sortedBy { it.lastModified }
                    4 -> allFiles.sortedByDescending { it.size }
                    5 -> allFiles.sortedBy { it.size }
                    else -> allFiles
                }
                applyFilter()
            }
            .show()
    }

    private fun getMimeType(file: StorageFile): String {
        return when (file.type) {
            StorageFile.FileType.IMAGE -> "image/*"
            StorageFile.FileType.VIDEO -> "video/*"
            StorageFile.FileType.AUDIO -> "audio/*"
            StorageFile.FileType.DOCUMENT -> "application/*"
            StorageFile.FileType.OTHER -> "*/*"
        }
    }
}