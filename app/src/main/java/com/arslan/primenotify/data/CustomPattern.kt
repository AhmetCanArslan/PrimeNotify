package com.arslan.primenotify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
data class CustomPattern(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("intervals")
    val intervals: List<Long> // true means flash ON duration, false is implicit by gaps between intervals.
    // Wait, let's represent intervals as [delayMs, onMs, delayMs, onMs...] like standard vibration pattern.
) : Parcelable
