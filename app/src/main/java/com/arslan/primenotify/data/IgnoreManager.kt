package com.arslan.primenotify.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IgnoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("prime_notify_ignore", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY = "ignore_rules"

    fun getRules(): List<IgnoreRule> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<IgnoreRule>>() {}.type
            gson.fromJson<List<IgnoreRule>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRule(rule: IgnoreRule) {
        val current = getRules().toMutableList()
        // Avoid duplicates
        val duplicate = current.any { it.type == rule.type && it.packageName == rule.packageName && it.matchValue == rule.matchValue }
        if (!duplicate) {
            current.add(rule)
            save(current)
        }
    }

    private fun save(rules: List<IgnoreRule>) {
        prefs.edit().putString(KEY, gson.toJson(rules)).apply()
    }

    /**
     * Returns true if the notification described by [packageName], [title], and [body]
     * is covered by at least one ignore rule.
     */
    fun isIgnored(packageName: String, title: String, body: String): Boolean =
        getRules().any { rule ->
            when (rule.type) {
                IgnoreType.APP ->
                    rule.packageName == packageName
                IgnoreType.TITLE ->
                    rule.packageName == packageName &&
                        rule.matchValue.isNotBlank() &&
                        title.contains(rule.matchValue, ignoreCase = true)
                IgnoreType.BODY ->
                    rule.packageName == packageName &&
                        rule.matchValue.isNotBlank() &&
                        body.contains(rule.matchValue, ignoreCase = true)
            }
        }
}
