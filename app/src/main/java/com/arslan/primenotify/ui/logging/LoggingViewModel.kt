package com.arslan.primenotify.ui.logging

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arslan.primenotify.data.AppListManager
import com.arslan.primenotify.data.IgnoreManager
import com.arslan.primenotify.data.IgnoreRule
import com.arslan.primenotify.data.IgnoreType
import com.arslan.primenotify.data.LogEntry
import com.arslan.primenotify.data.LoggingManager
import com.arslan.primenotify.data.LoggingPreferences
import com.arslan.primenotify.data.RuleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoggingViewModel(application: Application) : AndroidViewModel(application) {

    private val loggingManager = LoggingManager.getInstance(application)
    private val ignoreManager = IgnoreManager(application)
    val loggingPreferences = LoggingPreferences(application)

    // ---- observable state ----

    private val _filter = MutableStateFlow<RuleType?>(null)
    val filter: StateFlow<RuleType?> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Raw logs from [LoggingManager], before client-side search/preference filtering. */
    private val _rawLogs = MutableStateFlow<List<LogEntry>>(emptyList())

    /** Logs displayed to the user (search + preference filters applied). */
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _iconCache = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    val iconCache: StateFlow<Map<String, ImageBitmap?>> = _iconCache.asStateFlow()

    /** Dedicated trigger to force re-filtering without self-assigning _rawLogs. */
    private val _filterTrigger = MutableStateFlow(0)

    // Preference-change triggers
    private val _onlyRuleMatched = MutableStateFlow(loggingPreferences.onlyRuleMatched)
    val onlyRuleMatched: StateFlow<Boolean> = _onlyRuleMatched.asStateFlow()

    private val _showSystemApps = MutableStateFlow(loggingPreferences.showSystemApps)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    private val _autoDeleteDays = MutableStateFlow(loggingPreferences.autoDeleteDays)
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    init {
        // Auto-purge old entries on launch
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.purgeOlderThan(loggingPreferences.autoDeleteDays)
        }

        refreshLogs()

        // Observe installed-apps list; rebuild icon cache whenever it changes
        viewModelScope.launch {
            AppListManager.installedApps
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest { loaded ->
                    if (loaded) {
                        rebuildIconCache()
                        applyFilters()  // system-app filter depends on installed apps
                    }
                }
        }

        // Re-filter whenever search, prefs, or rawLogs change
        viewModelScope.launch {
            combine(_rawLogs, _searchQuery, _onlyRuleMatched, _showSystemApps, _filterTrigger) { args ->
                @Suppress("UNCHECKED_CAST")
                filterLogs(
                    args[0] as List<LogEntry>,
                    args[1] as String,
                    args[2] as Boolean,
                    args[3] as Boolean,
                )
            }.collectLatest { filtered ->
                _logs.value = filtered
            }
        }
    }

    // ---- public API ----

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setOnlyRuleMatched(value: Boolean) {
        loggingPreferences.onlyRuleMatched = value
        _onlyRuleMatched.value = value
    }

    fun setShowSystemApps(value: Boolean) {
        loggingPreferences.showSystemApps = value
        _showSystemApps.value = value
    }

    fun setAutoDeleteDays(days: Int) {
        loggingPreferences.autoDeleteDays = days
        _autoDeleteDays.value = days
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.purgeOlderThan(days)
            val result = loggingManager.getLogs(_filter.value)
            withContext(Dispatchers.Main) { _rawLogs.value = result }
        }
    }

    fun ignoreEntry(entry: LogEntry, type: IgnoreType) {
        val rule = when (type) {
            IgnoreType.APP -> IgnoreRule(
                type = IgnoreType.APP,
                packageName = entry.packageName,
                appName = entry.appName
            )
            IgnoreType.TITLE -> IgnoreRule(
                type = IgnoreType.TITLE,
                packageName = entry.packageName,
                appName = entry.appName,
                matchValue = entry.title
            )
            IgnoreType.BODY -> IgnoreRule(
                type = IgnoreType.BODY,
                packageName = entry.packageName,
                appName = entry.appName,
                matchValue = entry.body
            )
            IgnoreType.TITLE_AND_BODY -> IgnoreRule(
                type = IgnoreType.TITLE_AND_BODY,
                packageName = entry.packageName,
                appName = entry.appName,
                matchValue = entry.title,
                matchValue2 = entry.body
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.addRule(rule)
        }
    }

    /**
     * Create an ignore rule with a custom pattern (optionally regex).
     */
    fun ignoreEntryWithPattern(
        entry: LogEntry,
        type: IgnoreType,
        pattern: String,
        isRegex: Boolean
    ) {
        val rule = IgnoreRule(
            type = type,
            packageName = entry.packageName,
            appName = entry.appName,
            matchValue = pattern,
            isRegex = isRegex
        )
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.addRule(rule)
        }
    }

    /**
     * Create ignore rules from the unified ignore dialog.
     *
     * The app ([entry.packageName]) is ALWAYS the scope — rules only ever match
     * notifications from that specific app, never from other apps.
     *
     * - [titlePattern] non-null → create a TITLE rule (scoped to app)
     * - [bodyPattern]  non-null → create a BODY  rule (scoped to app)
     * - both null                → create an APP rule  (ignore ALL from this app)
     */
    fun ignoreFromDialog(
        entry: LogEntry,
        titlePattern: String?,
        titleIsRegex: Boolean,
        bodyPattern: String?,
        bodyIsRegex: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (titlePattern == null && bodyPattern == null) {
                // No content filters → ignore everything from this app
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.APP,
                        packageName = entry.packageName,
                        appName = entry.appName
                    )
                )
                return@launch
            }
            if (titlePattern != null && bodyPattern != null) {
                // Both title AND body provided → single combined rule that requires both to match
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.TITLE_AND_BODY,
                        packageName = entry.packageName,
                        appName = entry.appName,
                        matchValue = titlePattern,
                        isRegex = titleIsRegex,
                        matchValue2 = bodyPattern,
                        isRegex2 = bodyIsRegex
                    )
                )
                return@launch
            }
            if (titlePattern != null) {
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.TITLE,
                        packageName = entry.packageName,
                        appName = entry.appName,
                        matchValue = titlePattern,
                        isRegex = titleIsRegex
                    )
                )
            }
            if (bodyPattern != null) {
                ignoreManager.addRule(
                    IgnoreRule(
                        type = IgnoreType.BODY,
                        packageName = entry.packageName,
                        appName = entry.appName,
                        matchValue = bodyPattern,
                        isRegex = bodyIsRegex
                    )
                )
            }
        }
    }

    fun setFilter(type: RuleType?) {
        _filter.value = type
        refreshLogs()
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.clearLogs()
            withContext(Dispatchers.Main) {
                _rawLogs.value = emptyList()
                _iconCache.value = emptyMap()
            }
        }
    }

    fun deleteLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.deleteLog(id)
            val result = loggingManager.getLogs(_filter.value)
            withContext(Dispatchers.Main) { _rawLogs.value = result }
        }
    }

    // ---- private helpers ----

    private fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = loggingManager.getLogs(_filter.value)
            withContext(Dispatchers.Main) { _rawLogs.value = result }
            val icons = buildIconCache(result)
            withContext(Dispatchers.Main) { _iconCache.value = icons }
        }
    }

    private fun applyFilters() {
        _filterTrigger.value++
    }

    private fun filterLogs(
        raw: List<LogEntry>,
        query: String,
        onlyRuleMatched: Boolean,
        showSystemApps: Boolean
    ): List<LogEntry> {
        var result = raw

        // 1) Only rule-matched
        if (onlyRuleMatched) {
            result = result.filter { it.matchedRules.isNotEmpty() }
        }

        // 2) Hide system apps (non-launchable apps not in AppListManager)
        if (!onlyRuleMatched && !showSystemApps) {
            val knownPackages = AppListManager.installedApps.value.map { it.packageName }.toHashSet()
            if (knownPackages.isNotEmpty()) {
                result = result.filter { it.packageName in knownPackages }
            }
        }

        // 3) Search query
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter { entry ->
                entry.appName.lowercase().contains(q) ||
                    entry.title.lowercase().contains(q) ||
                    entry.body.lowercase().contains(q)
            }
        }
        return result
    }

    private suspend fun rebuildIconCache() {
        val currentLogs = _rawLogs.value
        if (currentLogs.isEmpty()) return
        val icons = withContext(Dispatchers.IO) { buildIconCache(currentLogs) }
        _iconCache.value = icons
    }

    private fun buildIconCache(logs: List<LogEntry>): Map<String, ImageBitmap?> {
        val packages = HashSet<String>(logs.size)
        for (entry in logs) packages.add(entry.packageName)
        return packages.associateWith { AppListManager.getIconForPackage(it) }
    }
}