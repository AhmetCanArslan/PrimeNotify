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
    
    @SerializedName("packageNames")
    val packageNames: List<String>,
    
    @SerializedName("appNames")
    val appNames: List<String>,
    
    @SerializedName("keyword")
    val keyword: String = "",
    
    @SerializedName("keywords")
    val keywords: List<String> = emptyList(),
    
    @SerializedName("pattern")
    val pattern: FlashPattern,
    
    @SerializedName("customPatternId")
    val customPatternId: String? = null,
    
    @SerializedName("isEnabled")
    var isEnabled: Boolean = true
) : Parcelable

@Keep
enum class FlashPattern(val displayName: String) {
    HEARTBEAT("Heartbeat"),
    PING_PONG("Ping Pong")
}
