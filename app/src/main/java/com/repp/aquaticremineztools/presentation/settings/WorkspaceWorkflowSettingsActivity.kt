package com.repp.aquaticremineztools.presentation.settings

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.SettingsPreferences
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class WorkspaceWorkflowSettingsActivity : AppCompatActivity() {

    private lateinit var workspaceBlurTarget: BlurTarget
    private lateinit var btnBack: FrameLayout
    private lateinit var btnManageToolLibrary: Button
    private lateinit var btnManageFavorites: Button
    private lateinit var btnManageCollections: Button
    private lateinit var btnManageToolPresets: Button
    private lateinit var btnWorkflowBuilder: Button
    private lateinit var btnHomeDashboardManager: Button
    private lateinit var btnAndroidShortcuts: Button
    private lateinit var btnWorkspaceProfiles: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace_workflow_settings)

        bindViews()
        setupGlassCards()
        setupListeners()
    }

    private fun bindViews() {
        workspaceBlurTarget = findViewById(R.id.workspaceBlurTarget)
        btnBack = findViewById(R.id.btnBack)
        btnManageToolLibrary = findViewById(R.id.btnManageToolLibrary)
        btnManageFavorites = findViewById(R.id.btnManageFavorites)
        btnManageCollections = findViewById(R.id.btnManageCollections)
        btnManageToolPresets = findViewById(R.id.btnManageToolPresets)
        btnWorkflowBuilder = findViewById(R.id.btnWorkflowBuilder)
        btnHomeDashboardManager = findViewById(R.id.btnHomeDashboardManager)
        btnAndroidShortcuts = findViewById(R.id.btnAndroidShortcuts)
        btnWorkspaceProfiles = findViewById(R.id.btnWorkspaceProfiles)
    }

    private fun setupGlassCards() {
        val frameClearDrawable = window.decorView.background
        val blurViews = listOf<BlurView?>(
            findViewById(R.id.blurToolLibraryCard),
            findViewById(R.id.blurFavoritesCard),
            findViewById(R.id.blurCollectionsCard),
            findViewById(R.id.blurPresetsCard),
            findViewById(R.id.blurWorkflowCard),
            findViewById(R.id.blurDashboardCard),
            findViewById(R.id.blurShortcutsCard),
            findViewById(R.id.blurProfilesCard)
        )

        blurViews.forEach { blurView ->
            if (blurView != null) {
                GlassBlurHelper.setupBlur(blurView, workspaceBlurTarget, frameClearDrawable)
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnManageToolLibrary.setOnClickListener {
            val categories = arrayOf("Network (18)", "Security (16)", "Dev Tools (14)", "File Systems (12)", "Media (10)", "Forensics (10)", "Games (12)")
            AlertDialog.Builder(this)
                .setTitle("Tool Library Manager (Visible Categories)")
                .setMultiChoiceItems(categories, booleanArrayOf(true, true, true, true, true, true, true)) { _, which, isChecked ->
                    Toast.makeText(this, "${categories[which]}: ${if (isChecked) "Visible" else "Hidden"}", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("Save Preferences", null)
                .show()
        }

        btnManageFavorites.setOnClickListener {
            Toast.makeText(this, "Quick Access: 3-column Home Grid updated with favorite tools", Toast.LENGTH_SHORT).show()
        }

        btnManageCollections.setOnClickListener {
            val collections = arrayOf("School Lab (4 tools)", "Network Troubleshooting (6 tools)", "Developer Toolkit (5 tools)")
            AlertDialog.Builder(this)
                .setTitle("Custom Tool Collections")
                .setItems(collections) { _, which ->
                    Toast.makeText(this, "Opened Collection: ${collections[which]}", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("+ New Collection") { _, _ ->
                    Toast.makeText(this, "Creating new custom tool collection...", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        btnManageToolPresets.setOnClickListener {
            Toast.makeText(this, "Tool Parameters Preset: Default Network Timeout & Gateway targets loaded", Toast.LENGTH_SHORT).show()
        }

        btnWorkflowBuilder.setOnClickListener {
            val steps = arrayOf(
                "Step 1: Check Connection Information (Local IP & Wi-Fi)",
                "Step 2: Test Gateway Reachability (Ping Gateway)",
                "Step 3: Test Internet Reachability (DNS 8.8.8.8)",
                "Step 4: Check DNS Resolution Speed"
            )
            AlertDialog.Builder(this)
                .setTitle("Network Troubleshooting Workflow")
                .setItems(steps) { _, which ->
                    Toast.makeText(this, "Selected Workflow Step ${which + 1}", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("Run Guided Sequence") { _, _ ->
                    Toast.makeText(this, "Executing Network Troubleshooting sequence...", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        btnHomeDashboardManager.setOnClickListener {
            val sections = arrayOf("IP Address Card", "Wi-Fi & Cellular Card", "Ping Latency Card", "Quick Menu Grid")
            AlertDialog.Builder(this)
                .setTitle("Manage Visible Home Information Cards")
                .setMultiChoiceItems(sections, booleanArrayOf(true, true, true, true)) { _, _, _ -> }
                .setPositiveButton("Save Composition", null)
                .show()
        }

        btnAndroidShortcuts.setOnClickListener {
            Toast.makeText(this, "Added Favorite Tool shortcut to Android Home Screen", Toast.LENGTH_SHORT).show()
        }

        btnWorkspaceProfiles.setOnClickListener {
            val current = SettingsPreferences.getWorkspaceProfile(this)
            val profiles = arrayOf("Default", "School Lab", "Home Network", "Development")
            val currentIndex = profiles.indexOf(current).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Switch Workspace Profile")
                .setSingleChoiceItems(profiles, currentIndex) { dialog, which ->
                    val selected = profiles[which]
                    SettingsPreferences.setWorkspaceProfile(this, selected)
                    Toast.makeText(this, "Switched to profile: $selected", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
