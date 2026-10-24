package com.repp.aquaticremineztools.core.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.presentation.about.AboutActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.security.PinChangeActivity
import com.repp.aquaticremineztools.presentation.security.PinSetupActivity
import com.repp.aquaticremineztools.presentation.settings.SettingsActivity
import com.repp.aquaticremineztools.presentation.settings.WorkspaceWorkflowSettingsActivity
import com.repp.aquaticremineztools.presentation.storage.StorageActivity
import eightbitlab.com.blurview.BlurView

object DrawerMenuHelper {

    fun setup(
        activity: Activity,
        drawerLayout: DrawerLayout,
        blurViews: List<BlurView?> = emptyList()
    ) {
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerStateChanged(newState: Int) {
                val autoUpdate = (newState == DrawerLayout.STATE_IDLE)
                blurViews.forEach { it?.setBlurAutoUpdate(autoUpdate) }
            }
        })

        fun closeDrawerAnd(action: () -> Unit) {
            drawerLayout.closeDrawer(GravityCompat.START)
            action()
        }

        fun navigateTo(targetClass: Class<*>) {
            if (activity::class.java == targetClass) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.closeDrawer(GravityCompat.START)
                val intent = Intent(activity, targetClass).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                ActivityTransition.startActivitySmooth(activity, intent)
            }
        }

        activity.findViewById<LinearLayout>(R.id.menuHome)?.setOnClickListener {
            navigateTo(HomeActivity::class.java)
        }

        activity.findViewById<LinearLayout>(R.id.menuSearch)?.setOnClickListener {
            navigateTo(SearchActivity::class.java)
        }

        activity.findViewById<LinearLayout>(R.id.menuHistory)?.setOnClickListener {
            navigateTo(HistoryActivity::class.java)
        }

        activity.findViewById<LinearLayout>(R.id.menuNews)?.setOnClickListener {
            navigateTo(com.repp.aquaticremineztools.presentation.news.NewsActivity::class.java)
        }

        activity.findViewById<LinearLayout>(R.id.menuFavorites)?.setOnClickListener {
            closeDrawerAnd {
                Toast.makeText(activity, "Favorites - Quick Access Available", Toast.LENGTH_SHORT).show()
            }
        }

        activity.findViewById<LinearLayout>(R.id.menuCategories)?.setOnClickListener {
            closeDrawerAnd {
                ActivityTransition.startActivitySmooth(activity, Intent(activity, WorkspaceWorkflowSettingsActivity::class.java))
            }
        }

        activity.findViewById<LinearLayout>(R.id.menuTiktok)?.setOnClickListener {
            openUrl(activity, activity.getString(R.string.drawer_tiktok_url))
        }

        activity.findViewById<LinearLayout>(R.id.menuInstagram)?.setOnClickListener {
            openUrl(activity, activity.getString(R.string.drawer_instagram_url))
        }

        activity.findViewById<LinearLayout>(R.id.menuGames)?.setOnClickListener {
            closeDrawerAnd {
                Toast.makeText(activity, "Games - Cyber Games Collection", Toast.LENGTH_SHORT).show()
            }
        }

        activity.findViewById<LinearLayout>(R.id.menuStorage)?.setOnClickListener {
            closeDrawerAnd {
                ActivityTransition.startActivitySmooth(activity, Intent(activity, StorageActivity::class.java))
            }
        }

        activity.findViewById<LinearLayout>(R.id.menuPassword)?.setOnClickListener {
            closeDrawerAnd {
                handlePasswordMenu(activity)
            }
        }

        activity.findViewById<LinearLayout>(R.id.menuAbout)?.setOnClickListener {
            navigateTo(AboutActivity::class.java)
        }

        activity.findViewById<LinearLayout>(R.id.menuSettings)?.setOnClickListener {
            navigateTo(SettingsActivity::class.java)
        }

        updatePasswordLabel(activity)
    }

    fun updatePasswordLabel(activity: Activity) {
        activity.findViewById<TextView>(R.id.tvPasswordLabel)?.let { label ->
            label.text = if (PinManager.hasPin(activity)) {
                activity.getString(R.string.drawer_change_password)
            } else {
                activity.getString(R.string.drawer_create_password)
            }
        }
    }

    fun handlePasswordMenu(activity: Activity) {
        if (PinManager.hasPin(activity)) {
            val options = arrayOf(
                activity.getString(R.string.drawer_change_password),
                activity.getString(R.string.drawer_delete_password)
            )
            AlertDialog.Builder(activity, R.style.Theme_AquaticReminezTools_Dialog)
                .setTitle(R.string.drawer_features)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> activity.startActivity(Intent(activity, PinChangeActivity::class.java))
                        1 -> confirmDeletePassword(activity)
                    }
                }
                .show()
        } else {
            activity.startActivity(Intent(activity, PinSetupActivity::class.java))
        }
    }

    private fun confirmDeletePassword(activity: Activity) {
        AlertDialog.Builder(activity, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle(R.string.delete_pin_title)
            .setMessage(R.string.delete_pin_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                PinManager.deletePin(activity)
                Toast.makeText(activity, "Password deleted", Toast.LENGTH_SHORT).show()
                updatePasswordLabel(activity)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openUrl(activity: Activity, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }
}
