package com.arslan.primenotify.ui.logging

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.arslan.primenotify.data.LogEntry
import com.arslan.primenotify.data.LoggingManager
import com.arslan.primenotify.data.RuleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoggingViewModel(application: Application) : AndroidViewModel(application) {

    private val loggingManager = LoggingManager(application)

    private val _filter = MutableStateFlow<RuleType?>(null)
    val filter: StateFlow<RuleType?> = _filter.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        refreshLogs()
    }

    fun setFilter(type: RuleType?) {
        _filter.value = type
        refreshLogs()
    }

    fun clearLogs() {
        loggingManager.clearLogs()
        refreshLogs()
    }

    private fun refreshLogs() {
        _logs.value = loggingManager.getLogs(_filter.value)
    }
}
