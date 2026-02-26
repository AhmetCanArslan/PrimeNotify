package com.arslan.primenotify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * A trigger-based notification rule.
 *
 * One rule has:
 *  - A **trigger**: one or more target apps + optional keyword filters
 *  - One or more **actions** to execute when the trigger matches (Flash, Wake Up, AOD)
 *  - Shared **conditions**: ringer-mode gates, throttle
 *
 * Replaces the old separate FlashRule / WakeUpRule / AodRule model which forced
 * users to create three separate rules to perform three actions on the same trigger.
 */
@Keep
@Parcelize
data class NotificationRule(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("packageNames")
    val packageNames: List<String>,

    @SerializedName("appNames")
    val appNames: List<String>,

    @SerializedName("keywords")
    val keywords: List<String> = emptyList(),

    /** At least one action must be present for the rule to do anything. */
    @SerializedName("actions")
    val actions: List<RuleAction>,

    @SerializedName("applyOnVibration")
    val applyOnVibration: Boolean = true,

    @SerializedName("applyOnSilent")
    val applyOnSilent: Boolean = true,

    @SerializedName("applyOnDND")
    val applyOnDND: Boolean = true,

    @SerializedName("preventMultipleNotifications")
    val preventMultipleNotifications: Boolean = false,

    @SerializedName("isEnabled")
    val isEnabled: Boolean = true,
) : Parcelable
