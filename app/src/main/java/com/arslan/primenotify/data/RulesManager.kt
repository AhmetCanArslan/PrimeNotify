package com.arslan.primenotify.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.util.UUID

class RulesManager(context: Context) {
    
    private val prefs = context.getSharedPreferences("prime_notify_rules", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val FLASH_RULES_KEY = "flash_rules_list"
    
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
                
                cleanRules.add(FlashRule(id, packageNames, appNames, keyword, keywords, pattern, isEnabled))
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
}
