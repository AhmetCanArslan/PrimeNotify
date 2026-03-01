package com.arslan.primenotify.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * Full-screen overlay activity that flashes the screen with a chosen colour.
 *
 * Launched by [com.arslan.primenotify.service.ScreenFlashManager].
 * Accepts extras:
 *  - [EXTRA_COLOR_ARGB]  – ARGB packed Long for the flash colour
 *  - [EXTRA_DURATION_SEC] – seconds to flash; -1 = until user taps the screen
 *
 * The screen alternates 250 ms colour / 250 ms black.  When duration expires or
 * the user taps (in "until interaction" mode) the activity finishes.
 *
 * Handles `ACTION_STOP_FLASH` broadcast so the manager / service can stop it remotely.
 */
class ScreenFlashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_COLOR_ARGB = "extra_color_argb"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val ACTION_STOP_FLASH = "com.arslan.primenotify.STOP_SCREEN_FLASH"
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen and turn the screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val colorArgb = intent.getLongExtra(EXTRA_COLOR_ARGB, 0xFFFF1744)
        val durationSec = intent.getIntExtra(EXTRA_DURATION_SEC, 5)
        val flashColor = Color(colorArgb)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_FLASH), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_FLASH))
        }

        setContent {
            ScreenFlashContent(
                flashColor = flashColor,
                durationSeconds = durationSec,
                onFinish = { finish() }
            )
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
private fun ScreenFlashContent(
    flashColor: Color,
    durationSeconds: Int,
    onFinish: () -> Unit,
) {
    var showColor by remember { mutableStateOf(true) }
    val untilInteraction = durationSeconds == -1

    // Flash cycle: 250ms colour ↔ 250ms black
    LaunchedEffect(Unit) {
        val totalMs = if (untilInteraction) Long.MAX_VALUE else durationSeconds * 1000L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < totalMs) {
            showColor = true
            delay(250)
            showColor = false
            delay(250)
        }
        onFinish()
    }

    val currentColor = if (showColor) flashColor else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentColor)
            .then(
                if (untilInteraction) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFinish
                    )
                } else {
                    Modifier
                }
            )
    )
}
