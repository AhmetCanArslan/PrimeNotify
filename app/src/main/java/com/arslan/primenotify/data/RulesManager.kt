package com.arslan.primenotify.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.util.UUID

class RulesManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("prime_notify_rules", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val FLASH_RULES_KEY = "flash_rules_list"
    private val CUSTOM_PATTERNS_KEY = "custom_patterns_list"
    private val HAS_PROXIMITY_SENSOR_KEY = "has_proximity_sensor"

    fun hasProximitySensor(): Boolean {
        if (!prefs.contains(HAS_PROXIMITY_SENSOR_KEY)) {
            val hasSensor = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_PROXIMITY)
            prefs.edit().putBoolean(HAS_PROXIMITY_SENSOR_KEY, hasSensor).apply()
        }
        return prefs.getBoolean(HAS_PROXIMITY_SENSOR_KEY, true)
    }
    
    fun getFlashRules(): List<FlashRule> {
        val json = prefs.getString(FLASH_RULES_KEY, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val cleanRules = mutableListOf<FlashRule>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                
                val packageNames = mutableListOf<String>()
                if (obj.has("packageNames")) {
                    val arr = obj.getJSONArray("packageNames")
                    for (j in 0 until arr.length()) packageNames.add(arr.getString(j))
                } else if (obj.has("packageName")) {
                    packageNames.add(obj.getString("packageName"))
                }
                
                val appNames = mutableListOf<String>()
                if (obj.has("appNames")) {
                    val arr = obj.getJSONArray("appNames")
                    for (j in 0 until arr.length()) appNames.add(arr.getString(j))
                } else if (obj.has("appName")) {
                    appNames.add(obj.getString("appName"))
                }
                
                val keyword = obj.optString("keyword", "")
                val keywords = mutableListOf<String>()
                if (obj.has("keywords")) {
                    val arr = obj.getJSONArray("keywords")
                    for (j in 0 until arr.length()) keywords.add(arr.getString(j))
                } else if (keyword.isNotBlank()) {
                    keywords.add(keyword)
                }
                val patternStr = obj.optString("pattern", "HEARTBEAT")
                val pattern = try { FlashPattern.valueOf(patternStr) } catch (e: Exception) { FlashPattern.HEARTBEAT }
                val isEnabled = obj.optBoolean("isEnabled", true)
                val applyOnVibration = obj.optBoolean("applyOnVibration", true)
                val applyOnSilent = obj.optBoolean("applyOnSilent", true)
                val applyOnDND = obj.optBoolean("applyOnDND", true)
                val customPatternStr = obj.optString("customPatternId", "")
                val customPatternId = if (customPatternStr.isNotEmpty()) customPatternStr else null
                
                cleanRules.add(FlashRule(id, packageNames, appNames, keyword, keywords, pattern, customPatternId, applyOnVibration, applyOnSilent, applyOnDND, isEnabled))
            }
            cleanRules
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveFlashRules(rules: List<FlashRule>) {
        val json = gson.toJson(rules)
        prefs.edit().putString(FLASH_RULES_KEY, json).apply()
    }
    
    fun addFlashRule(rule: FlashRule) {
        val currentRules = getFlashRules().toMutableList()
        currentRules.add(rule)
        saveFlashRules(currentRules)
    }
    
    fun removeFlashRule(ruleId: String) {
        val currentRules = getFlashRules().toMutableList()
        currentRules.removeAll { it.id == ruleId }
        saveFlashRules(currentRules)
    }
    
    fun toggleFlashRule(ruleId: String, isEnabled: Boolean) {
        val currentRules = getFlashRules().toMutableList()
        val index = currentRules.indexOfFirst { it.id == ruleId }
        if (index != -1) {
            currentRules[index] = currentRules[index].copy(isEnabled = isEnabled)
            saveFlashRules(currentRules)
        }
    }
    
    fun updateFlashRule(updatedRule: FlashRule) {
        val currentRules = getFlashRules().toMutableList()
        val index = currentRules.indexOfFirst { it.id == updatedRule.id }
        if (index != -1) {
            currentRules[index] = updatedRule
            saveFlashRules(currentRules)
        }
    }
    
    fun getCustomPatterns(): List<CustomPattern> {
        val json = prefs.getString(CUSTOM_PATTERNS_KEY, null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<CustomPattern>>() {}.type
            gson.fromJson<List<CustomPattern>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveCustomPattern(pattern: CustomPattern) {
        val current = getCustomPatterns().toMutableList()
        val index = current.indexOfFirst { it.id == pattern.id }
        if (index != -1) current[index] = pattern else current.add(pattern)
        prefs.edit().putString(CUSTOM_PATTERNS_KEY, gson.toJson(current)).apply()
    }
    
    fun removeCustomPattern(id: String) {
        val current = getCustomPatterns().toMutableList()
        current.removeAll { it.id == id }
        prefs.edit().putString(CUSTOM_PATTERNS_KEY, gson.toJson(current)).apply()
    }
    
    // Wake Up Rules
    
    private val WAKE_UP_RULES_KEY = "wake_up_rules_list"
    
    fun getWakeUpRules(): List<WakeUpRule> {
        val json = prefs.getString(WAKE_UP_RULES_KEY, null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<WakeUpRule>>() {}.type
            gson.fromJson<List<WakeUpRule>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveWakeUpRules(rules: List<WakeUpRule>) {
        prefs.edit().putString(WAKE_UP_RULES_KEY, gson.toJson(rules)).apply()
    }
    
    fun addWakeUpRule(rule: WakeUpRule) {
        val current = getWakeUpRules().toMutableList()
        current.add(rule)
        saveWakeUpRules(current)
    }
    
    fun removeWakeUpRule(ruleId: String) {
        val current = getWakeUpRules().toMutableList()
        current.removeAll { it.id == ruleId }
        saveWakeUpRules(current)
    }
    
    fun toggleWakeUpRule(ruleId: String, isEnabled: Boolean) {
        val current = getWakeUpRules().toMutableList()
        val index = current.indexOfFirst { it.id == ruleId }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = isEnabled)
            saveWakeUpRules(current)
        }
    }
    
    fun updateWakeUpRule(updatedRule: WakeUpRule) {
        val current = getWakeUpRules().toMutableList()
        val index = current.indexOfFirst { it.id == updatedRule.id }
        if (index != -1) {
            current[index] = updatedRule
            saveWakeUpRules(current)
        }
    }
    // AOD Rules
    
    private val AOD_RULES_KEY = "aod_rules_list"
    
    fun getAodRules(): List<AodRule> {
        val json = prefs.getString(AOD_RULES_KEY, null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<AodRule>>() {}.type
            gson.fromJson<List<AodRule>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveAodRules(rules: List<AodRule>) {
        prefs.edit().putString(AOD_RULES_KEY, gson.toJson(rules)).apply()
    }
    
    fun addAodRule(rule: AodRule) {
        val current = getAodRules().toMutableList()
        current.add(rule)
        saveAodRules(current)
    }
    
    fun removeAodRule(ruleId: String) {
        val current = getAodRules().toMutableList()
        current.removeAll { it.id == ruleId }
        saveAodRules(current)
    }
    
    fun toggleAodRule(ruleId: String, isEnabled: Boolean) {
        val current = getAodRules().toMutableList()
        val index = current.indexOfFirst { it.id == ruleId }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = isEnabled)
            saveAodRules(current)
        }
    }
    
    fun updateAodRule(updatedRule: AodRule) {
        val current = getAodRules().toMutableList()
        val index = current.indexOfFirst { it.id == updatedRule.id }
        if (index != -1) {
            current[index] = updatedRule
            saveAodRules(current)
        }
    }
}
