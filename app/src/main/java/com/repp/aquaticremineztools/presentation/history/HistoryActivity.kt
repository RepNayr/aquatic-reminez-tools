package com.repp.aquaticremineztools.presentation.history

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import com.repp.aquaticremineztools.data.local.history.HistoryEntity
import com.repp.aquaticremineztools.data.local.history.HistoryRepository
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import com.repp.aquaticremineztools.core.util.ActivityTransition
import eightbitlab.com.blurview.BlurTarget
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Stack

class HistoryActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var homeBlurTarget: BlurTarget
    private lateinit var repository: HistoryRepository

    // Toolbar Normal vs Selection
    private lateinit var normalToolbar: View
    private lateinit var selectionToolbar: View
    private lateinit var btnMenu: FrameLayout
    private lateinit var btnMoreOptions: FrameLayout
    private lateinit var btnCloseSelection: FrameLayout
    private lateinit var tvSelectionCount: TextView
    private lateinit var btnSelectAll: TextView
    private lateinit var btnDeleteSelected: FrameLayout

    // Header & Search
    private lateinit var tvRecordCount: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var btnFilter: LinearLayout
    private lateinit var tvFilterBadge: TextView
    private lateinit var btnSort: LinearLayout
    private lateinit var tvSortLabel: TextView
    private lateinit var tvClearAllFilters: TextView
    private lateinit var svActiveFilters: View
    private lateinit var cgActiveFilters: ChipGroup

    // Timeline & States
    private lateinit var loadingView: ProgressBar
    private lateinit var rvHistoryTimeline: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var noResultsContainer: LinearLayout
    private lateinit var btnResetNoResults: com.google.android.material.button.MaterialButton

    // Bottom Nav
    private lateinit var tabHome: FrameLayout
    private lateinit var tabSearch: FrameLayout
    private lateinit var tabHistory: FrameLayout
    private lateinit var tabAbout: FrameLayout
    private lateinit var tabSettings: FrameLayout

    private lateinit var pillHome: LinearLayout
    private lateinit var pillSearch: LinearLayout
    private lateinit var pillHistory: LinearLayout
    private lateinit var pillAbout: LinearLayout
    private lateinit var pillSettings: LinearLayout

    private lateinit var iconHome: ImageView
    private lateinit var iconSearch: ImageView
    private lateinit var iconHistory: ImageView
    private lateinit var iconAbout: ImageView
    private lateinit var iconSettings: ImageView

    private lateinit var textHome: TextView
    private lateinit var textSearch: TextView
    private lateinit var textHistory: TextView
    private lateinit var textAbout: TextView
    private lateinit var textSettings: TextView

    // State data
    private var allHistory = listOf<HistoryEntity>()
    private var filteredHistory = listOf<HistoryEntity>()
    private val selectedIds = mutableSetOf<Long>()
    private var isSelectionMode = false

    private var searchQuery = ""
    private var filterOptions = HistoryFilterOptions()
    private var sortOrderNewestFirst = true

    // Undo / Redo Stacks
    private val undoStack = Stack<List<HistoryEntity>>()
    private val redoStack = Stack<List<HistoryEntity>>()

    private lateinit var timelineAdapter: HistoryTimelineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_history)

        repository = HistoryRepository.getInstance(this)

        bindViews()
        setupClicks()
        setupBottomNav()
        setupDrawerMenuClicks()
        setupRecyclerView()

        lifecycleScope.launch {
            repository.cleanupOldRecords()
            loadHistoryData()
        }
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        homeBlurTarget = findViewById(R.id.homeBlurTarget)

        normalToolbar = findViewById(R.id.normalToolbar)
        selectionToolbar = findViewById(R.id.selectionToolbar)
        btnMenu = findViewById(R.id.btnMenu)
        btnMoreOptions = findViewById(R.id.btnMoreOptions)
        btnCloseSelection = findViewById(R.id.btnCloseSelection)
        tvSelectionCount = findViewById(R.id.tvSelectionCount)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)

        tvRecordCount = findViewById(R.id.tvRecordCount)
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnFilter = findViewById(R.id.btnFilter)
        tvFilterBadge = findViewById(R.id.tvFilterBadge)
        btnSort = findViewById(R.id.btnSort)
        tvSortLabel = findViewById(R.id.tvSortLabel)
        tvClearAllFilters = findViewById(R.id.tvClearAllFilters)
        svActiveFilters = findViewById(R.id.svActiveFilters)
        cgActiveFilters = findViewById(R.id.cgActiveFilters)

        loadingView = findViewById(R.id.loadingView)
        rvHistoryTimeline = findViewById(R.id.rvHistoryTimeline)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        noResultsContainer = findViewById(R.id.noResultsContainer)
        btnResetNoResults = findViewById(R.id.btnResetNoResults)

        // Bottom Nav
        tabHome = findViewById(R.id.tabHome)
        tabSearch = findViewById(R.id.tabSearch)
        tabHistory = findViewById(R.id.tabHistory)
        tabAbout = findViewById(R.id.tabAbout)
        tabSettings = findViewById(R.id.tabSettings)

        pillHome = findViewById(R.id.pillHome)
        pillSearch = findViewById(R.id.pillSearch)
        pillHistory = findViewById(R.id.pillHistory)
        pillAbout = findViewById(R.id.pillAbout)
        pillSettings = findViewById(R.id.pillSettings)

        iconHome = findViewById(R.id.iconHome)
        iconSearch = findViewById(R.id.iconSearch)
        iconHistory = findViewById(R.id.iconHistory)
        iconAbout = findViewById(R.id.iconAbout)
        iconSettings = findViewById(R.id.iconSettings)

        textHome = findViewById(R.id.textHome)
        textSearch = findViewById(R.id.textSearch)
        textHistory = findViewById(R.id.textHistory)
        textAbout = findViewById(R.id.textAbout)
        textSettings = findViewById(R.id.textSettings)
    }

    private fun setupClicks() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnMoreOptions.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Delete All History")
            if (!undoStack.isEmpty()) {
                popup.menu.add(0, 2, 0, "Undo Delete")
            }
            if (!redoStack.isEmpty()) {
                popup.menu.add(0, 3, 0, "Redo Delete")
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> confirmDeleteAll()
                    2 -> performUndo()
                    3 -> performRedo()
                }
                true
            }
            popup.show()
        }

        btnCloseSelection.setOnClickListener {
            exitSelectionMode()
        }

        btnSelectAll.setOnClickListener {
            if (selectedIds.size == filteredHistory.size) {
                selectedIds.clear()
            } else {
                selectedIds.clear()
                filteredHistory.forEach { selectedIds.add(it.id) }
            }
            updateSelectionUI()
        }

        btnDeleteSelected.setOnClickListener {
            confirmDeleteSelected()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                filterAndProcessData()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        btnFilter.setOnClickListener {
            val sheet = HistoryFilterBottomSheet(filterOptions, onApply = { newOpts ->
                filterOptions = newOpts
                filterAndProcessData()
                updateActiveFilterChips()
            }, onReset = {
                filterOptions = HistoryFilterOptions()
                filterAndProcessData()
                updateActiveFilterChips()
            })
            sheet.show(supportFragmentManager, "HistoryFilter")
        }

        btnSort.setOnClickListener {
            sortOrderNewestFirst = !sortOrderNewestFirst
            tvSortLabel.text = if (sortOrderNewestFirst) "Newest first" else "Oldest first"
            filterAndProcessData()
        }

        tvClearAllFilters.setOnClickListener {
            filterOptions = HistoryFilterOptions()
            etSearch.setText("")
            filterAndProcessData()
            updateActiveFilterChips()
        }

        btnResetNoResults.setOnClickListener {
            filterOptions = HistoryFilterOptions()
            etSearch.setText("")
            filterAndProcessData()
            updateActiveFilterChips()
        }
    }

    private fun performUndo() {
        if (undoStack.isEmpty()) return
        val itemsToRestore = undoStack.pop()
        lifecycleScope.launch {
            itemsToRestore.forEach { repository.insert(it) }
            redoStack.push(itemsToRestore)
            Toast.makeText(this@HistoryActivity, "Action undone", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRedo() {
        if (redoStack.isEmpty()) return
        val itemsToReDelete = redoStack.pop()
        lifecycleScope.launch {
            repository.deleteByIds(itemsToReDelete.map { it.id })
            undoStack.push(itemsToReDelete)
            Toast.makeText(this@HistoryActivity, "Action redone", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        selectNavTab(2)

        tabHome.setOnClickListener {
            ActivityTransition.finishSmooth(this)
        }

        tabSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
            finish()
        }

        tabHistory.setOnClickListener {
            // Already here
        }

        tabAbout.setOnClickListener {
            Toast.makeText(this, "About - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        tabSettings.setOnClickListener {
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectNavTab(index: Int) {
        val tabContainers = listOf(tabHome, tabSearch, tabHistory, tabAbout, tabSettings)
        val tabs = listOf(
            Triple(pillHome, iconHome, textHome),
            Triple(pillSearch, iconSearch, textSearch),
            Triple(pillHistory, iconHistory, textHistory),
            Triple(pillAbout, iconAbout, textAbout),
            Triple(pillSettings, iconSettings, textSettings),
        )

        val primaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        tabs.forEachIndexed { i, (pill, icon, text) ->
            val container = tabContainers[i]
            val lp = container.layoutParams as? LinearLayout.LayoutParams
            if (i == index) {
                lp?.weight = 1.4f
                container.layoutParams = lp
                container.requestLayout()
                pill.setBackgroundResource(R.drawable.bg_nav_active_pill)
                text.visibility = View.VISIBLE
                icon.setColorFilter(primaryColor)
            } else {
                lp?.weight = 0.9f
                container.layoutParams = lp
                container.requestLayout()
                pill.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                text.visibility = View.GONE
                icon.setColorFilter(secondaryColor)
            }
        }
    }

    private fun setupDrawerMenuClicks() {
        findViewById<LinearLayout>(R.id.menuHome).setNavigatorClick {
            ActivityTransition.finishSmooth(this)
        }

        findViewById<LinearLayout>(R.id.menuSearch).setNavigatorClick {
            closeDrawer()
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.menuHistory).setNavigatorClick {
            closeDrawer()
        }

        findViewById<LinearLayout>(R.id.menuFavorites).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Favorites - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuCategories).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Categories - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuStorage).setNavigatorClick {
            closeDrawer()
            ActivityTransition.startActivitySmooth(this, Intent(this, StorageActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.menuSettings).setNavigatorClick {
            closeDrawer()
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun View.setNavigatorClick(action: () -> Unit) {
        setOnClickListener {
            closeDrawer()
            action()
        }
    }

    private fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupRecyclerView() {
        timelineAdapter = HistoryTimelineAdapter(
            blurTarget = homeBlurTarget,
            frameClearDrawable = window.decorView.background,
            onItemClick = { entity ->
                val detailSheet = HistoryDetailBottomSheet(
                    entity = entity,
                    onDelete = { toDel ->
                        lifecycleScope.launch {
                            undoStack.push(listOf(toDel))
                            redoStack.clear()
                            repository.delete(toDel)
                            Toast.makeText(this@HistoryActivity, "Activity deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenTool = { ent ->
                        Toast.makeText(this@HistoryActivity, "Opening ${ent.toolName}...", Toast.LENGTH_SHORT).show()
                        com.repp.aquaticremineztools.core.history.HistoryLogger.logOpened(
                            this@HistoryActivity, ent.toolId, ent.toolName, ent.category
                        )
                    }
                )
                detailSheet.show(supportFragmentManager, "HistoryDetail")
            },
            onItemLongClick = { entity ->
                if (!isSelectionMode) {
                    enterSelectionMode(entity.id)
                }
            },
            onCheckBoxChanged = { entity, isChecked ->
                if (isChecked) {
                    selectedIds.add(entity.id)
                } else {
                    selectedIds.remove(entity.id)
                }
                updateSelectionUI()
            },
            onOverflowClick = { entity, view ->
                val popup = PopupMenu(this, view)
                popup.menu.add(0, 1, 0, "Delete")
                popup.setOnMenuItemClickListener { item ->
                    if (item.itemId == 1) {
                        if (entity.status == "RUNNING") {
                            Toast.makeText(this, "Cannot delete ongoing execution", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                undoStack.push(listOf(entity))
                                redoStack.clear()
                                repository.delete(entity)
                                Toast.makeText(this@HistoryActivity, "Deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    true
                }
                popup.show()
            }
        )

        rvHistoryTimeline.layoutManager = LinearLayoutManager(this)
        rvHistoryTimeline.adapter = timelineAdapter
    }

    private suspend fun loadHistoryData() {
        loadingView.visibility = View.VISIBLE
        repository.getAllHistory().collectLatest { list ->
            loadingView.visibility = View.GONE
            allHistory = list
            filterAndProcessData()
        }
    }

    private fun filterAndProcessData() {
        var result = allHistory.asSequence()

        if (searchQuery.isNotEmpty()) {
            result = result.filter { it.toolName.contains(searchQuery, ignoreCase = true) }
        }

        filterOptions.category?.let { cat ->
            result = result.filter { it.category == cat }
        }

        filterOptions.activityType?.let { type ->
            result = result.filter { it.activityType == type }
        }

        filterOptions.status?.let { st ->
            result = result.filter { it.status == st }
        }

        filterOptions.dateRange?.let { range ->
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            when (range) {
                "TODAY" -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val startOfDay = cal.timeInMillis
                    result = result.filter { it.timestamp >= startOfDay }
                }
                "LAST_7" -> {
                    val cutoff = now - 7L * 24 * 60 * 60 * 1000
                    result = result.filter { it.timestamp >= cutoff }
                }
                "LAST_30" -> {
                    val cutoff = now - 30L * 24 * 60 * 60 * 1000
                    result = result.filter { it.timestamp >= cutoff }
                }
            }
        }

        filteredHistory = if (sortOrderNewestFirst) {
            result.sortedByDescending { it.timestamp }.toList()
        } else {
            result.sortedBy { it.timestamp }.toList()
        }

        tvRecordCount.text = "${allHistory.size} records"

        val timelineItems = buildTimelineItems(filteredHistory)
        timelineAdapter.submitItems(timelineItems)

        if (allHistory.isEmpty()) {
            emptyStateContainer.visibility = View.VISIBLE
            noResultsContainer.visibility = View.GONE
            rvHistoryTimeline.visibility = View.GONE
        } else if (filteredHistory.isEmpty()) {
            emptyStateContainer.visibility = View.GONE
            noResultsContainer.visibility = View.VISIBLE
            rvHistoryTimeline.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            noResultsContainer.visibility = View.GONE
            rvHistoryTimeline.visibility = View.VISIBLE
        }
    }

    private fun buildTimelineItems(entities: List<HistoryEntity>): List<HistoryTimelineItem> {
        val items = mutableListOf<HistoryTimelineItem>()
        var lastDateString = ""

        val todayStr = getFormattedDateHeader(System.currentTimeMillis())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = getFormattedDateHeader(yesterdayCal.timeInMillis)

        for (entity in entities) {
            val entityDateStr = getFormattedDateHeader(entity.timestamp)
            val headerText = when (entityDateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> entityDateStr
            }

            if (headerText != lastDateString) {
                items.add(HistoryTimelineItem.Header(headerText))
                lastDateString = headerText
            }

            items.add(HistoryTimelineItem.Card(entity, selectedIds.contains(entity.id)))
        }

        return items
    }

    private fun getFormattedDateHeader(timeMs: Long): String {
        return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timeMs))
    }

    private fun updateActiveFilterChips() {
        cgActiveFilters.removeAllViews()
        val activeCount = listOf(
            filterOptions.category,
            filterOptions.activityType,
            filterOptions.status,
            filterOptions.dateRange
        ).count { it != null }

        if (activeCount > 0) {
            tvFilterBadge.visibility = View.VISIBLE
            tvFilterBadge.text = activeCount.toString()
            tvClearAllFilters.visibility = View.VISIBLE
            svActiveFilters.visibility = View.VISIBLE

            val addChip = { label: String, onRemove: () -> Unit ->
                val chip = Chip(this).apply {
                    text = label
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { onRemove() }
                    setChipBackgroundColorResource(R.color.bg_card)
                    setTextColor(ContextCompat.getColor(this@HistoryActivity, R.color.text_primary))
                }
                cgActiveFilters.addView(chip)
            }

            filterOptions.category?.let { cat -> addChip(cat) { filterOptions = filterOptions.copy(category = null); filterAndProcessData(); updateActiveFilterChips() } }
            filterOptions.activityType?.let { type -> addChip(if (type == "EXECUTED") "Executed" else "Opened") { filterOptions = filterOptions.copy(activityType = null); filterAndProcessData(); updateActiveFilterChips() } }
            filterOptions.status?.let { st -> addChip(st) { filterOptions = filterOptions.copy(status = null); filterAndProcessData(); updateActiveFilterChips() } }
            filterOptions.dateRange?.let { dr ->
                val label = when(dr) { "TODAY" -> "Today"; "LAST_7" -> "Last 7 days"; else -> "Last 30 days" }
                addChip(label) { filterOptions = filterOptions.copy(dateRange = null); filterAndProcessData(); updateActiveFilterChips() }
            }
        } else {
            tvFilterBadge.visibility = View.GONE
            tvClearAllFilters.visibility = View.GONE
            svActiveFilters.visibility = View.GONE
        }
    }

    private fun enterSelectionMode(initialId: Long) {
        isSelectionMode = true
        timelineAdapter.isSelectionMode = true
        selectedIds.clear()
        selectedIds.add(initialId)

        normalToolbar.visibility = View.GONE
        selectionToolbar.visibility = View.VISIBLE
        updateSelectionUI()
        timelineAdapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        timelineAdapter.isSelectionMode = false
        selectedIds.clear()

        normalToolbar.visibility = View.VISIBLE
        selectionToolbar.visibility = View.GONE
        timelineAdapter.notifyDataSetChanged()
    }

    private fun updateSelectionUI() {
        val count = selectedIds.size
        tvSelectionCount.text = "$count selected"
        btnSelectAll.text = if (count == filteredHistory.size && filteredHistory.isNotEmpty()) "Deselect All" else "Select All"
        timelineAdapter.notifyDataSetChanged()
    }

    private fun confirmDeleteSelected() {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Delete Selected Activities")
            .setMessage("Are you sure you want to delete ${selectedIds.size} selected activity records?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val itemsToDelete = allHistory.filter { selectedIds.contains(it.id) }
                    undoStack.push(itemsToDelete)
                    redoStack.clear()
                    repository.deleteByIds(selectedIds.toList())
                    Toast.makeText(this@HistoryActivity, "${selectedIds.size} records deleted", Toast.LENGTH_SHORT).show()
                    exitSelectionMode()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Delete All History")
            .setMessage("Are you sure you want to clear all history records? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                lifecycleScope.launch {
                    val allCurrent = allHistory.toList()
                    undoStack.push(allCurrent)
                    redoStack.clear()
                    repository.deleteAll()
                    Toast.makeText(this@HistoryActivity, "All history cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode()
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onResume() {
        super.onResume()
        selectNavTab(2)
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
