package com.arslan.primenotify.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RulesManager(context: Context) {
    
    private val prefs = context.getSharedPreferences("prime_notify_rules", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val FLASH_RULES_KEY = "flash_rules_list"
    
    fun getFlashRules(): List<FlashRule> {
        val json = prefs.getString(FLASH_RULES_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<FlashRule>>() {}.type
        return try {
            gson.fromJson(json, type)
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
}
