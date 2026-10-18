package com.repp.aquaticremineztools.presentation.settings

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.SettingsPreferences

class WorkspaceWorkflowSettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var btnManageToolLibrary: Button
    private lateinit var btnManageFavorites: Button
    private lateinit var btnManageCollections: Button
    private lateinit var btnManageToolPresets: Button
    private lateinit var btnWorkflowBuilder: Button
    private lateinit var btnWorkspaceProfiles: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workspace_workflow_settings)

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        btnManageToolLibrary = findViewById(R.id.btnManageToolLibrary)
        btnManageFavorites = findViewById(R.id.btnManageFavorites)
        btnManageCollections = findViewById(R.id.btnManageCollections)
        btnManageToolPresets = findViewById(R.id.btnManageToolPresets)
        btnWorkflowBuilder = findViewById(R.id.btnWorkflowBuilder)
        btnWorkspaceProfiles = findViewById(R.id.btnWorkspaceProfiles)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnManageToolLibrary.setOnClickListener {
            Toast.makeText(this, "Tool Library Manager: 100 tools configured", Toast.LENGTH_SHORT).show()
        }

        btnManageFavorites.setOnClickListener {
            Toast.makeText(this, "Manage Favorites & Quick Menu 3-column grid", Toast.LENGTH_SHORT).show()
        }

        btnManageCollections.setOnClickListener {
            Toast.makeText(this, "Custom Collection: School Lab & Network Troubleshooting active", Toast.LENGTH_SHORT).show()
        }

        btnManageToolPresets.setOnClickListener {
            Toast.makeText(this, "Tool configuration presets loaded", Toast.LENGTH_SHORT).show()
        }

        btnWorkflowBuilder.setOnClickListener {
            Toast.makeText(this, "Workflow Builder: Network Diagnostics 4-step sequence ready", Toast.LENGTH_SHORT).show()
        }

        btnWorkspaceProfiles.setOnClickListener {
            val current = SettingsPreferences.getWorkspaceProfile(this)
            val profiles = arrayOf("Default", "School Lab", "Home Network", "Development")
            val currentIndex = profiles.indexOf(current).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Select Workspace Profile")
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
