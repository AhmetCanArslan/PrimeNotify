package com.arslan.primenotify.ui.ignored

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arslan.primenotify.data.IgnoreManager
import com.arslan.primenotify.data.IgnoreRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IgnoredNotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val ignoreManager = IgnoreManager(application)

    private val _rules = MutableStateFlow<List<IgnoreRule>>(emptyList())
    val rules: StateFlow<List<IgnoreRule>> = _rules.asStateFlow()

    init {
        refresh()
    }

    fun removeRule(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ignoreManager.removeRule(id)
            val updated = ignoreManager.getRules()
            withContext(Dispatchers.Main) { _rules.value = updated }
        }
    }

    private fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ignoreManager.getRules()
            withContext(Dispatchers.Main) { _rules.value = result }
        }
    }
}
