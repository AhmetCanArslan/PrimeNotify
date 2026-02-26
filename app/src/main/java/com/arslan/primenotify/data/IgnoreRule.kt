package com.arslan.primenotify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class IgnoreType { APP, TITLE, BODY }

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
     * For [IgnoreType.TITLE] / [IgnoreType.BODY] this is the substring to match.
     */
    @SerializedName("matchValue")
    val matchValue: String? = null
) : Parcelable
