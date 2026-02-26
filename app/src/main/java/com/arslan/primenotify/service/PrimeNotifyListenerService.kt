package com.arslan.primenotify.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.arslan.primenotify.data.LoggingManager
import com.arslan.primenotify.data.MatchedRuleInfo
import com.arslan.primenotify.data.RuleType
import com.arslan.primenotify.data.RulesManager

class PrimeNotifyListenerService : NotificationListenerService() {
    
    private lateinit var rulesManager: RulesManager
    private lateinit var flashManager: FlashManager
    private lateinit var screenWakeManager: ScreenWakeManager
    private lateinit var aodManager: AodManager
    private lateinit var loggingManager: LoggingManager

    override fun onCreate() {
        super.onCreate()
        rulesManager = RulesManager(this)
        flashManager = FlashManager(this)
        screenWakeManager = ScreenWakeManager(this)
        aodManager = AodManager(this)
        loggingManager = LoggingManager(this)
    }

    override fun onDestroy() {
        flashManager.stop()
        screenWakeManager.stop()
        aodManager.stop()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!isPrimeNotifyServiceEnabled(this) || sbn == null) {
            return
        }

        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val searchBody = "$title $text $bigText".lowercase()

        // Read ringer/DND state once, shared across all rule checks
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = am.ringerMode
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val interruptionFilter = nm.currentInterruptionFilter

        val isVibration = ringerMode == AudioManager.RINGER_MODE_VIBRATE
        val isSilent = ringerMode == AudioManager.RINGER_MODE_SILENT
        val isDND = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        val loggedRules = mutableListOf<MatchedRuleInfo>()

        // ── Flash rule matching ───────────────────────────────────────────────
        val activeFlashRules = rulesManager.getFlashRules().filter { it.isEnabled }

        val matchedFlashRule = activeFlashRules.firstOrNull { rule ->
            rule.packageNames.contains(packageName) &&
                (rule.keywords.isEmpty() || rule.keywords.any { kw -> searchBody.contains(kw.lowercase()) })
        }

        if (matchedFlashRule != null) {
            var shouldExecute = true
            if (matchedFlashRule.preventMultipleNotifications && rulesManager.shouldThrottleRule(matchedFlashRule.id)) {
                shouldExecute = false
            }
            if (isVibration && !matchedFlashRule.applyOnVibration) shouldExecute = false
            if (isSilent && !matchedFlashRule.applyOnSilent) shouldExecute = false
            if (isDND && !matchedFlashRule.applyOnDND) shouldExecute = false

            if (shouldExecute) {
                rulesManager.updateRuleExecutionTime(matchedFlashRule.id)
                val customPattern = matchedFlashRule.customPatternId?.let { id ->
                    rulesManager.getCustomPatterns().find { it.id == id }
                }
                if (customPattern != null) {
                    flashManager.executeCustomPattern(customPattern.intervals)
                } else {
                    flashManager.executePattern(matchedFlashRule.pattern)
                }
            }

            loggedRules.add(
                MatchedRuleInfo(
                    ruleId = matchedFlashRule.id,
                    ruleName = buildRuleLabel(matchedFlashRule.appNames, matchedFlashRule.keywords),
                    ruleType = RuleType.FLASH,
                    wasExecuted = shouldExecute
                )
            )
        }

        // ── Wake-up rule matching ─────────────────────────────────────────────
        val activeWakeUpRules = rulesManager.getWakeUpRules().filter { it.isEnabled }

        val matchedWakeUpRule = activeWakeUpRules.firstOrNull { rule ->
            rule.packageNames.contains(packageName) &&
                (rule.keywords.isEmpty() || rule.keywords.any { kw -> searchBody.contains(kw.lowercase()) })
        }

        if (matchedWakeUpRule != null) {
            var shouldExecute = true
            if (matchedWakeUpRule.preventMultipleNotifications && rulesManager.shouldThrottleRule(matchedWakeUpRule.id)) {
                shouldExecute = false
            }
            if (isVibration && !matchedWakeUpRule.applyOnVibration) shouldExecute = false
            if (isSilent && !matchedWakeUpRule.applyOnSilent) shouldExecute = false
            if (isDND && !matchedWakeUpRule.applyOnDND) shouldExecute = false

            if (shouldExecute) {
                rulesManager.updateRuleExecutionTime(matchedWakeUpRule.id)
                screenWakeManager.wakeScreen(
                    durationSeconds = matchedWakeUpRule.screenDurationSeconds,
                    pocketModeEnabled = matchedWakeUpRule.pocketModeEnabled
                )
            }

            loggedRules.add(
                MatchedRuleInfo(
                    ruleId = matchedWakeUpRule.id,
                    ruleName = buildRuleLabel(matchedWakeUpRule.appNames, matchedWakeUpRule.keywords),
                    ruleType = RuleType.WAKE_UP,
                    wasExecuted = shouldExecute
                )
            )
        }

        // ── AOD rule matching ─────────────────────────────────────────────────
        val activeAodRules = rulesManager.getAodRules().filter { it.isEnabled }

        val matchedAodRule = activeAodRules.firstOrNull { rule ->
            rule.packageNames.contains(packageName) &&
                (rule.keywords.isEmpty() || rule.keywords.any { kw -> searchBody.contains(kw.lowercase()) })
        }

        if (matchedAodRule != null) {
            var shouldExecute = true
            if (matchedAodRule.preventMultipleNotifications && rulesManager.shouldThrottleRule(matchedAodRule.id)) {
                shouldExecute = false
            }
            if (isVibration && !matchedAodRule.applyOnVibration) shouldExecute = false
            if (isSilent && !matchedAodRule.applyOnSilent) shouldExecute = false
            if (isDND && !matchedAodRule.applyOnDND) shouldExecute = false

            if (shouldExecute) {
                rulesManager.updateRuleExecutionTime(matchedAodRule.id)
                aodManager.triggerAod(durationSeconds = matchedAodRule.durationSeconds)
            }

            loggedRules.add(
                MatchedRuleInfo(
                    ruleId = matchedAodRule.id,
                    ruleName = buildRuleLabel(matchedAodRule.appNames, matchedAodRule.keywords),
                    ruleType = RuleType.AOD,
                    wasExecuted = shouldExecute
                )
            )
        }

        // ── Log once if at least one rule matched ─────────────────────────────
        if (loggedRules.isNotEmpty()) {
            val appName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
            loggingManager.logNotification(
                packageName = packageName,
                appName = appName,
                title = title,
                body = bigText.ifBlank { text },
                matchedRules = loggedRules
            )
        }

        super.onNotificationPosted(sbn)
    }

    /**
     * Builds a human-readable rule label from its target app names and keywords.
     * Examples: "WhatsApp", "WhatsApp, Telegram", "WhatsApp – work, urgent"
     */
    private fun buildRuleLabel(appNames: List<String>, keywords: List<String>): String {
        val appsLabel = appNames.joinToString(", ").ifBlank { "?" }
        return if (keywords.isEmpty()) appsLabel else "$appsLabel – ${keywords.joinToString(", ")}"
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null && isPrimeNotifyServiceEnabled(this)) {
            val activeAodRules = rulesManager.getAodRules().filter { it.isEnabled && it.durationSeconds == -1 }
            val matchedAodRule = activeAodRules.firstOrNull { rule ->
                rule.packageNames.contains(sbn.packageName)
            }
            if (matchedAodRule != null) {
                aodManager.stopAodForReason(-1)
            }
        }
    }
}
