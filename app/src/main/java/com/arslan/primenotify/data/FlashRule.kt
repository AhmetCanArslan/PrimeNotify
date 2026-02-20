package com.arslan.primenotify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
data class FlashRule(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerializedName("packageName")
    val packageName: String,
    
    @SerializedName("appName")
    val appName: String,
    
    @SerializedName("keyword")
    val keyword: String,
    
    @SerializedName("pattern")
    val pattern: FlashPattern,
    
    @SerializedName("isEnabled")
    var isEnabled: Boolean = true
) : Parcelable

@Keep
enum class FlashPattern(val displayName: String) {
    HEARTBEAT("Heartbeat"),
    PING_PONG("Ping Pong")
}
