package com.arslan.primenotify.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.arslan.primenotify.data.ScreenFlashColor
import com.arslan.primenotify.ui.ScreenFlashActivity

/**
 * Launches [ScreenFlashActivity] as a full-screen overlay to flash the screen
 * with the given colour.  If the screen is off the wake lock turns it on first.
 *
 * Calling [triggerFlash] while a previous flash is still active will stop the
 * old one via a local broadcast before starting the new one.
 */
class ScreenFlashManager(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    fun triggerFlash(color: ScreenFlashColor, durationSeconds: Int) {
        // Stop any currently running flash overlay
        stop()

        val isScreenOff = !powerManager.isInteractive
        val isDeviceLocked = keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked

        // Only flash when screen is off/closed or device is locked
        if (!isScreenOff && !isDeviceLocked) return

        // If screen is off, wake it briefly so the activity becomes visible
        if (isScreenOff) {
            @Suppress("DEPRECATION")
            val wl = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                    or PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.ON_AFTER_RELEASE,
                "PrimeNotify:ScreenFlashWake"
            )
            wl.acquire(3_000L) // hold just long enough for the activity to start
        }

        val intent = Intent(context, ScreenFlashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ScreenFlashActivity.EXTRA_COLOR_ARGB, color.colorArgb)
            putExtra(ScreenFlashActivity.EXTRA_DURATION_SEC, durationSeconds)
        }
        context.startActivity(intent)
    }

    /** Stops any currently running ScreenFlashActivity via broadcast. */
    fun stop() {
        context.sendBroadcast(Intent(ScreenFlashActivity.ACTION_STOP_FLASH))
    }
}
