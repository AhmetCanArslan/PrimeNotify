package com.arslan.primenotify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.arslan.primenotify.R
import com.arslan.primenotify.data.FlashPattern
import com.arslan.primenotify.data.IgnoreManager
import com.arslan.primenotify.data.LoggingManager
import com.arslan.primenotify.data.LoggingPreferences
import com.arslan.primenotify.data.MatchedRuleInfo
import com.arslan.primenotify.data.RuleType
import com.arslan.primenotify.data.RulesManager

class PrimeNotifyListenerService : NotificationListenerService() {

    private lateinit var rulesManager: RulesManager
    private lateinit var ignoreManager: IgnoreManager
    private lateinit var flashManager: FlashManager
    private lateinit var screenWakeManager: ScreenWakeManager
    private lateinit var aodManager: AodManager
    private lateinit var loggingManager: LoggingManager
    private lateinit var loggingPreferences: LoggingPreferences

    override fun onCreate() {
        super.onCreate()
        rulesManager = RulesManager(this)
        ignoreManager = IgnoreManager(this)
        flashManager = FlashManager(this)
        screenWakeManager = ScreenWakeManager(this)
        aodManager = AodManager(this)
        loggingManager = LoggingManager(this)
        loggingPreferences = LoggingPreferences(this)
        startPersistentNotification()
    }

    override fun onDestroy() {
        flashManager.stop()
        screenWakeManager.stop()
        aodManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!isPrimeNotifyServiceEnabled(this) || sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val bodyRaw = bigText.ifBlank { text }

        // Skip notification entirely if it matches an ignore rule
        if (ignoreManager.isIgnored(packageName, title, bodyRaw)) {
            super.onNotificationPosted(sbn)
            return
        }

        val searchBody = "$title $text $bigText".lowercase()
        val titleLower = title.lowercase()
        val bodyLower = "$text $bigText".lowercase()

        // Read ringer/DND state once for all rule checks
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = am.ringerMode
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val interruptionFilter = nm.currentInterruptionFilter

        val isVibration = ringerMode == AudioManager.RINGER_MODE_VIBRATE
        val isSilent = ringerMode == AudioManager.RINGER_MODE_SILENT
        val isDND = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        val allLoggedRules = mutableListOf<MatchedRuleInfo>()

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        for (rule in rulesManager.getRules().filter { it.isEnabled }) {
            val isMatch = rule.packageNames.contains(packageName) &&
                (rule.keywords.isEmpty() || rule.keywords.any { kw -> searchBody.contains(kw.lowercase()) }) &&
                (rule.titleKeywords.isEmpty() || rule.titleKeywords.any { kw -> titleLower.contains(kw.lowercase()) }) &&
                (rule.bodyKeywords.isEmpty() || rule.bodyKeywords.any { kw -> bodyLower.contains(kw.lowercase()) })
            if (!isMatch) continue

            // Evaluate shared conditions once per rule
            var shouldExecute = true
            if (rule.preventMultipleNotifications && rulesManager.shouldThrottleRule(rule.id)) {
                shouldExecute = false
            }
            if (isVibration && !rule.applyOnVibration) shouldExecute = false
            if (isSilent && !rule.applyOnSilent) shouldExecute = false
            if (isDND && !rule.applyOnDND) shouldExecute = false

            if (shouldExecute) rulesManager.updateRuleExecutionTime(rule.id)

            val ruleLabel = buildRuleLabel(rule.appNames, rule.keywords, rule.titleKeywords, rule.bodyKeywords)

            for (action in rule.actions) {
                if (shouldExecute) {
                    when (action.type) {
                        RuleType.FLASH -> {
                            val customPattern = action.customPatternId?.let { id ->
                                rulesManager.getCustomPatterns().find { it.id == id }
                            }
                            if (customPattern != null) {
                                flashManager.executeCustomPattern(customPattern.intervals)
                            } else {
                                flashManager.executePattern(action.flashPattern ?: FlashPattern.HEARTBEAT)
                            }
                        }
                        RuleType.WAKE_UP -> {
                            screenWakeManager.wakeScreen(
                                durationSeconds = action.screenDurationSeconds ?: 10,
                                pocketModeEnabled = action.pocketModeEnabled ?: true,
                            )
                        }
                        RuleType.AOD -> {
                            aodManager.triggerAod(durationSeconds = action.aodDurationSeconds ?: 10)
                        }
                    }
                }
                allLoggedRules.add(
                    MatchedRuleInfo(
                        ruleId = rule.id,
                        ruleName = ruleLabel,
                        ruleType = action.type,
                        wasExecuted = shouldExecute,
                    )
                )
            }
        }

        // Auto-purge old entries
        loggingManager.purgeOlderThan(loggingPreferences.autoDeleteDays)

        // Only log if rule matched OR user wants all notifications
        if (allLoggedRules.isNotEmpty() || !loggingPreferences.onlyRuleMatched) {
            loggingManager.logNotification(
                packageName = packageName,
                appName = appName,
                title = title,
                body = bodyRaw,
                matchedRules = allLoggedRules,
            )
        }

        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null && isPrimeNotifyServiceEnabled(this)) {
            val matched = rulesManager.getRules().filter { it.isEnabled }.firstOrNull { rule ->
                rule.packageNames.contains(sbn.packageName) &&
                    rule.actions.any { it.type == RuleType.AOD && it.aodDurationSeconds == -1 }
            }
            if (matched != null) aodManager.stopAodForReason(-1)
        }
    }

    /** E.g. "WhatsApp", "WhatsApp, Telegram", "WhatsApp – title:urgent, body:invoice" */
    private fun buildRuleLabel(appNames: List<String>, keywords: List<String>, titleKeywords: List<String>, bodyKeywords: List<String>): String {
        val appsLabel = appNames.joinToString(", ").ifBlank { "?" }
        val parts = mutableListOf<String>()
        if (keywords.isNotEmpty()) parts.add(keywords.joinToString(", "))
        if (titleKeywords.isNotEmpty()) parts.add("title: ${titleKeywords.joinToString("|")}") 
        if (bodyKeywords.isNotEmpty()) parts.add("body: ${bodyKeywords.joinToString("|")}")  
        return if (parts.isEmpty()) appsLabel else "$appsLabel \u2013 ${parts.joinToString("; ")}"
    }

    private fun startPersistentNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            PERSISTENT_CHANNEL_ID,
            getString(R.string.service_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_persistent_text)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(getString(R.string.notification_persistent_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        startForeground(PERSISTENT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val PERSISTENT_CHANNEL_ID = "prime_notify_persistent"
        private const val PERSISTENT_NOTIFICATION_ID = 1001
    }
}
