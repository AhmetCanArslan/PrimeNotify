package com.arslan.primenotify.ui.logging

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arslan.primenotify.data.AppListManager
import com.arslan.primenotify.data.LogEntry
import com.arslan.primenotify.data.LoggingManager
import com.arslan.primenotify.data.RuleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoggingViewModel(application: Application) : AndroidViewModel(application) {

    private val loggingManager = LoggingManager(application)

    private val _filter = MutableStateFlow<RuleType?>(null)
    val filter: StateFlow<RuleType?> = _filter.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _iconCache = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    val iconCache: StateFlow<Map<String, ImageBitmap?>> = _iconCache.asStateFlow()

    init {
        refreshLogs()
    }

    fun setFilter(type: RuleType?) {
        _filter.value = type
        refreshLogs()
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            loggingManager.clearLogs()
            val result = loggingManager.getLogs(null)
            val icons = buildIconCache(result)
            withContext(Dispatchers.Main) {
                _logs.value = result
                _iconCache.value = icons
            }
        }
    }

    private fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = loggingManager.getLogs(_filter.value)
            val icons = buildIconCache(result)
            withContext(Dispatchers.Main) {
                _logs.value = result
                _iconCache.value = icons
            }
        }
    }

    private fun buildIconCache(logs: List<LogEntry>): Map<String, ImageBitmap?> =
        logs.map { it.packageName }
            .distinct()
            .associateWith { AppListManager.getIconForPackage(it) }
}