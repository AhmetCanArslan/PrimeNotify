package com.arslan.primenotify.data

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Keep
class LoggingManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // Lazy-loaded so Gson deserialization happens on the first IO-thread call
    // instead of blocking the main thread during ViewModel construction.
    private val entries: MutableList<LogEntry> by lazy { loadFromPrefs() }

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
        // Deduplication: skip if identical notification was logged within the window
        val now = System.currentTimeMillis()
        val isDuplicate = entries.any { e ->
            e.packageName == packageName && e.title == title && e.body == body &&
                (now - e.timestamp) < DEDUP_WINDOW_MS
        }
        if (isDuplicate) return

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
    fun deleteLog(id: String) {
        entries.removeAll { it.id == id }
        persist()
    }

    @Synchronized
    fun clearLogs() {
        entries.clear()
        persist()
    }

    /**
     * Removes entries older than [days] days.
     * No-op if [days] <= 0.
     */
    @Synchronized
    fun purgeOlderThan(days: Int) {
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        val before = entries.size
        entries.removeAll { it.timestamp < cutoff }
        if (entries.size != before) persist()
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
        private const val DEDUP_WINDOW_MS = 3_000L
    }
}
