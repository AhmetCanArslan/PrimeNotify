package com.arslan.primenotify.data

import android.content.Context

/**
 * Persisted preferences for the Logging screen.
 *
 * Stored in a dedicated SharedPreferences file so they don't collide with
 * the log entries themselves.
 */
class LoggingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Number of days after which log entries are automatically deleted. 0 = never. */
    var autoDeleteDays: Int
        get() = prefs.getInt(KEY_AUTO_DELETE_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, value).apply()

    /** When true, only show notifications that matched at least one rule. */
    var onlyRuleMatched: Boolean
        get() = prefs.getBoolean(KEY_ONLY_RULE_MATCHED, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLY_RULE_MATCHED, value).apply()

    /** When true, show notifications from system / non-launchable apps. */
    var showSystemApps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, value).apply()

    companion object {
        private const val PREFS_NAME = "prime_notify_log_prefs"
        private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
        private const val KEY_ONLY_RULE_MATCHED = "only_rule_matched"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
    }
}
