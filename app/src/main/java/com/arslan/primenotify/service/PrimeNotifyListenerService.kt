package com.arslan.primenotify.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.arslan.primenotify.data.RulesManager

class PrimeNotifyListenerService : NotificationListenerService() {
    
    private lateinit var rulesManager: RulesManager
    private lateinit var flashManager: FlashManager
    private lateinit var screenWakeManager: ScreenWakeManager
    
    override fun onCreate() {
        super.onCreate()
        rulesManager = RulesManager(this)
        flashManager = FlashManager(this)
        screenWakeManager = ScreenWakeManager(this)
    }

    override fun onDestroy() {
        flashManager.stop()
        screenWakeManager.stop()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!isPrimeNotifyServiceEnabled(this) || sbn == null) {
            return
        }
        
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        // Grab the title and content text to search for the keyword
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
        val searchBody = "$title $text $bigText".lowercase()
        
        // Flash rule matching
        val activeFlashRules = rulesManager.getFlashRules().filter { it.isEnabled }
        
        val matchedFlashRule = activeFlashRules.firstOrNull { rule ->
            val isAppMatch = rule.packageNames.contains(packageName)
            val isKeywordMatch = rule.keywords.isEmpty() || rule.keywords.any { kw ->
                searchBody.contains(kw.lowercase())
            }
            isAppMatch && isKeywordMatch
        }
        
        if (matchedFlashRule != null) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = am.ringerMode
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val filter = nm.currentInterruptionFilter

            val isVibration = ringerMode == AudioManager.RINGER_MODE_VIBRATE
            val isSilent = ringerMode == AudioManager.RINGER_MODE_SILENT
            val isDND = filter != NotificationManager.INTERRUPTION_FILTER_ALL

            var shouldExecute = true
            if (isVibration && !matchedFlashRule.applyOnVibration) shouldExecute = false
            if (isSilent && !matchedFlashRule.applyOnSilent) shouldExecute = false
            if (isDND && !matchedFlashRule.applyOnDND) shouldExecute = false
            
            if (shouldExecute) {
                val customPattern = matchedFlashRule.customPatternId?.let { id ->
                    rulesManager.getCustomPatterns().find { it.id == id }
                }
                if (customPattern != null) {
                    flashManager.executeCustomPattern(customPattern.intervals)
                } else {
                    flashManager.executePattern(matchedFlashRule.pattern)
                }
            }
        }
        
        // Wake-up rule matching
        val activeWakeUpRules = rulesManager.getWakeUpRules().filter { it.isEnabled }
        
        val matchedWakeUpRule = activeWakeUpRules.firstOrNull { rule ->
            val isAppMatch = rule.packageNames.contains(packageName)
            val isKeywordMatch = rule.keywords.isEmpty() || rule.keywords.any { kw ->
                searchBody.contains(kw.lowercase())
            }
            isAppMatch && isKeywordMatch
        }
        
        if (matchedWakeUpRule != null) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = am.ringerMode
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val filter = nm.currentInterruptionFilter

            val isVibration = ringerMode == AudioManager.RINGER_MODE_VIBRATE
            val isSilent = ringerMode == AudioManager.RINGER_MODE_SILENT
            val isDND = filter != NotificationManager.INTERRUPTION_FILTER_ALL

            var shouldExecute = true
            if (isVibration && !matchedWakeUpRule.applyOnVibration) shouldExecute = false
            if (isSilent && !matchedWakeUpRule.applyOnSilent) shouldExecute = false
            if (isDND && !matchedWakeUpRule.applyOnDND) shouldExecute = false
            
            if (shouldExecute) {
                screenWakeManager.wakeScreen(
                    durationSeconds = matchedWakeUpRule.screenDurationSeconds,
                    pocketModeEnabled = matchedWakeUpRule.pocketModeEnabled
                )
            }
        }
        
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
