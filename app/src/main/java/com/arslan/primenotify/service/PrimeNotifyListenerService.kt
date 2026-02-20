package com.arslan.primenotify.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.arslan.primenotify.data.RulesManager

class PrimeNotifyListenerService : NotificationListenerService() {
    
    private lateinit var rulesManager: RulesManager
    private lateinit var flashManager: FlashManager
    
    override fun onCreate() {
        super.onCreate()
        rulesManager = RulesManager(this)
        flashManager = FlashManager(this)
    }

    override fun onDestroy() {
        flashManager.stop()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!isPrimeNotifyServiceEnabled(this) || sbn == null) {
            return
        }
        
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        // Grab the title and content text to search for the keyword
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
        val searchBody = "$title $text $bigText".lowercase()
        
        val activeRules = rulesManager.getFlashRules().filter { it.isEnabled }
        
        val matchedRule = activeRules.firstOrNull { rule ->
            val isAppMatch = rule.packageNames.contains(packageName)
            val isKeywordMatch = rule.keywords.isEmpty() || rule.keywords.any { kw ->
                title.contains(kw, ignoreCase = true) || 
                text.contains(kw, ignoreCase = true)
            }
            isAppMatch && isKeywordMatch && rule.isEnabled
        }
        
        if (matchedRule != null) {
            val customPattern = matchedRule.customPatternId?.let { id ->
                rulesManager.getCustomPatterns().find { it.id == id }
            }
            if (customPattern != null) {
                flashManager.executeCustomPattern(customPattern.intervals)
            } else {
                flashManager.executePattern(matchedRule.pattern)
            }
        }
        
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
