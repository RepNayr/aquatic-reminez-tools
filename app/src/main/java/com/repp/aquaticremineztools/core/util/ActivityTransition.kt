package com.repp.aquaticremineztools.core.util

import android.app.Activity
import android.content.Intent
import com.repp.aquaticremineztools.R

object ActivityTransition {
    fun startActivitySmooth(activity: Activity, intent: Intent) {
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    fun finishSmooth(activity: Activity) {
        activity.finish()
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
