package com.arslan.primenotify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class IgnoreType { APP, TITLE, BODY, TITLE_AND_BODY }

@Keep
@Parcelize
data class IgnoreRule(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("type")
    val type: IgnoreType,

    /** Package name of the app this rule applies to. */
    @SerializedName("packageName")
    val packageName: String,

    /** Human-readable app name (for display in the ignored list). */
    @SerializedName("appName")
    val appName: String? = null,

    /**
     * For [IgnoreType.APP] this is unused (match is by packageName alone).
     * For [IgnoreType.TITLE] / [IgnoreType.BODY] this is the substring to match
     * (or a regex pattern when [isRegex] is true).
     */
    @SerializedName("matchValue")
    val matchValue: String? = null,

    /**
     * When true, [matchValue] is treated as a regex pattern
     * instead of a plain substring.
     */
    @SerializedName("isRegex")
    val isRegex: Boolean = false,

    /**
     * For [IgnoreType.TITLE_AND_BODY] only: the body substring/pattern to match
     * alongside [matchValue] (which is the title). Both must match for the rule
     * to fire.
     */
    @SerializedName("matchValue2")
    val matchValue2: String? = null,

    /** When true, [matchValue2] is treated as a regex pattern. */
    @SerializedName("isRegex2")
    val isRegex2: Boolean = false
) : Parcelable
