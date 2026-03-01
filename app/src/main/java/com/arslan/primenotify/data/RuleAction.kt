package com.arslan.primenotify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Represents a single action to execute when a [NotificationRule] is triggered.
 * Uses a flat structure with nullable action-specific fields for Gson compatibility.
 */
@Keep
@Parcelize
data class RuleAction(
    @SerializedName("type")
    val type: RuleType,

    // ── Flash-specific ────────────────────────────────────────────────────────
    @SerializedName("flashPattern")
    val flashPattern: FlashPattern? = null,

    @SerializedName("customPatternId")
    val customPatternId: String? = null,

    // ── Wake Up-specific ──────────────────────────────────────────────────────
    /** Seconds to keep the screen on. 0 = system default. */
    @SerializedName("screenDurationSeconds")
    val screenDurationSeconds: Int? = null,

    @SerializedName("pocketModeEnabled")
    val pocketModeEnabled: Boolean? = null,

    // ── AOD-specific ──────────────────────────────────────────────────────────
    /** Seconds for AOD. -1 = until notification dismissed. -2 = until phone unlocked. */
    @SerializedName("aodDurationSeconds")
    val aodDurationSeconds: Int? = null,

    // ── Flash Screen-specific ─────────────────────────────────────────────────
    /** Enum name of [ScreenFlashColor]. */
    @SerializedName("screenFlashColor")
    val screenFlashColor: String? = null,

    /** Seconds the screen flashes. -1 = until user taps the screen. */
    @SerializedName("screenFlashDurationSeconds")
    val screenFlashDurationSeconds: Int? = null,
) : Parcelable {

    companion object {
        fun flash(
            pattern: FlashPattern = FlashPattern.HEARTBEAT,
            customPatternId: String? = null,
        ) = RuleAction(
            type = RuleType.FLASH,
            flashPattern = pattern,
            customPatternId = customPatternId,
        )

        fun wakeUp(
            screenDurationSeconds: Int = 10,
            pocketModeEnabled: Boolean = true,
        ) = RuleAction(
            type = RuleType.WAKE_UP,
            screenDurationSeconds = screenDurationSeconds,
            pocketModeEnabled = pocketModeEnabled,
        )

        fun aod(durationSeconds: Int = 10) = RuleAction(
            type = RuleType.AOD,
            aodDurationSeconds = durationSeconds,
        )

        fun flashScreen(
            color: ScreenFlashColor = ScreenFlashColor.RED,
            durationSeconds: Int = 5,
        ) = RuleAction(
            type = RuleType.FLASH_SCREEN,
            screenFlashColor = color.name,
            screenFlashDurationSeconds = durationSeconds,
        )
    }
}
