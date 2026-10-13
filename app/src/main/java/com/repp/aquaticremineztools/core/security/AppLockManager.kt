package com.repp.aquaticremineztools.core.security

import android.app.Activity
import android.content.Context
import android.content.Intent

object AppLockManager {

    private var lastPausedTime = 0L
    private const val LOCK_TIMEOUT_MS = 30_000L

    fun onActivityPaused() {
        lastPausedTime = System.currentTimeMillis()
    }

    fun shouldLock(context: Context): Boolean {
        if (!PinManager.hasPin(context)) return false
        if (!PinManager.isLockEnabled(context)) return false
        if (lastPausedTime == 0L) return false
        val timeSincePaused = System.currentTimeMillis() - lastPausedTime
        return timeSincePaused > LOCK_TIMEOUT_MS
    }

    fun resetPauseTimer() {
        lastPausedTime = 0L
    }

    fun redirectToVerify(context: Context, targetActivityClass: Class<out Activity>) {
        val intent = Intent(context, targetActivityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }
}