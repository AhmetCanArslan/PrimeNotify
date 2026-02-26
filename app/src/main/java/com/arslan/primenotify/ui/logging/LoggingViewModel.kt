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
import com.arslan.primenotify.data.RuleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoggingViewModel(application: Application) : AndroidViewModel(application) {

    private val loggingManager = LoggingManager(application)
    private val ignoreManager = IgnoreManager(application)

    private val _filter = MutableStateFlow<RuleType?>(null)
    val filter: StateFlow<RuleType?> = _filter.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _iconCache = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    val iconCache: StateFlow<Map<String, ImageBitmap?>> = _iconCache.asStateFlow()

    init {
        refreshLogs()
        // Observe installed-apps list; rebuild icon cache whenever it changes
        // (e.g. when AppListManager finishes its async load).
        viewModelScope.launch {
            AppListManager.installedApps
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest { loaded ->
                    if (loaded) rebuildIconCache()
                }
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
        }
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.addRule(rule)
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
                _logs.value = emptyList()
                _iconCache.value = emptyMap()
            }
        }
    }

    fun deleteLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.deleteLog(id)
            val result = loggingManager.getLogs(_filter.value)
            withContext(Dispatchers.Main) {
                _logs.value = result
            }
        }
    }

    /** Load logs immediately, then build icon cache separately so the list appears fast. */
    private fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = loggingManager.getLogs(_filter.value)
            withContext(Dispatchers.Main) {
                _logs.value = result
            }
            // build icon cache without blocking list display
            val icons = buildIconCache(result)
            withContext(Dispatchers.Main) {
                _iconCache.value = icons
            }
        }
    }

    /** Rebuild icons from the current log list (called when AppListManager finishes loading). */
    private suspend fun rebuildIconCache() {
        val currentLogs = _logs.value
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