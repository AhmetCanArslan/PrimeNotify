package com.arslan.primenotify.data

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Keep
class LoggingManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // Loaded once at construction; kept in sync with every write so reads never re-parse.
    private val entries: MutableList<LogEntry> = loadFromPrefs()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Appends a new [LogEntry] for a notification that matched at least one rule.
     * Automatically evicts the oldest entry when the cap is exceeded.
     * This method is @Synchronized so it is safe to call from any thread
     * (the binder thread used by NotificationListenerService, UI thread, etc.).
     */
    @Synchronized
    fun logNotification(
        packageName: String,
        appName: String,
        title: String,
        body: String,
        matchedRules: List<MatchedRuleInfo>
    ) {
        val entry = LogEntry(
            packageName = packageName,
            appName = appName,
            title = title,
            body = body,
            matchedRules = matchedRules
        )
        entries.add(entry)
        // FIFO eviction: keep at most MAX_ENTRIES
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
        persist()
    }

    /**
     * Returns a copy of the log list, newest first.
     * Pass a [RuleType] to filter to entries that matched a rule of that type.
     */
    @Synchronized
    fun getLogs(filter: RuleType? = null): List<LogEntry> {
        val source = if (filter == null) {
            entries
        } else {
            entries.filter { entry -> entry.matchedRules.any { it.ruleType == filter } }
        }
        return source.reversed()
    }

    /** Clears all persisted and in-memory log entries. */
    @Synchronized
    fun clearLogs() {
        entries.clear()
        persist()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun persist() {
        val json = gson.toJson(entries)
        prefs.edit().putString(LOG_ENTRIES_KEY, json).apply()
    }

    private fun loadFromPrefs(): MutableList<LogEntry> {
        val json = prefs.getString(LOG_ENTRIES_KEY, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<LogEntry>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    companion object {
        private const val PREFS_NAME = "prime_notify_logs"
        private const val LOG_ENTRIES_KEY = "log_entries_list"
        private const val MAX_ENTRIES = 500
    }
}
