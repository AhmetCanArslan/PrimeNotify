package com.arslan.primenotify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
data class WakeUpRule(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerializedName("packageNames")
    val packageNames: List<String>,
    
    @SerializedName("appNames")
    val appNames: List<String>,
    
    @SerializedName("keywords")
    val keywords: List<String> = emptyList(),
    
    @SerializedName("screenDurationSeconds")
    val screenDurationSeconds: Int = 10,
    
    @SerializedName("pocketModeEnabled")
    val pocketModeEnabled: Boolean = true,
    
    @SerializedName("applyOnVibration")
    val applyOnVibration: Boolean = true,
    
    @SerializedName("applyOnSilent")
    val applyOnSilent: Boolean = true,
    
    @SerializedName("applyOnDND")
    val applyOnDND: Boolean = true,
    
    @SerializedName("isEnabled")
    var isEnabled: Boolean = true
) : Parcelable
