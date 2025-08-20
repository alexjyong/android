package chat.revolt.internals

import android.content.Context
import chat.revolt.RevoltApplication
import chat.revolt.api.RevoltJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class EmojiUsageData(
    val usageCount: Map<String, Int> = emptyMap(),
    val lastUsed: Map<String, Long> = emptyMap()
)

object EmojiUsageTracker {
    private const val PREFS_NAME = "emoji_usage"
    private const val KEY_DATA = "usage_data"
    
    private var usageCount = mutableMapOf<String, Int>()
    private var lastUsed = mutableMapOf<String, Long>()
    
    init {
        loadFromStorage()
    }
    
    fun recordUsage(emoji: String) {
        usageCount[emoji] = (usageCount[emoji] ?: 0) + 1
        lastUsed[emoji] = System.currentTimeMillis()
        saveToStorage()
    }
    
    fun getRecentlyUsed(limit: Int = 12): List<String> {
        if (lastUsed.isEmpty()) return emptyList()
        
        return lastUsed.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
    
    fun getMostUsed(limit: Int = 12): List<String> {
        if (usageCount.isEmpty()) return emptyList()
        
        return usageCount.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { lastUsed[it.key] ?: 0 }
            )
            .take(limit)
            .map { it.key }
    }
    
    private fun loadFromStorage() {
        try {
            val prefs = RevoltApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonData = prefs.getString(KEY_DATA, null) ?: return
            
            val data = RevoltJson.decodeFromString<EmojiUsageData>(jsonData)
            usageCount.putAll(data.usageCount)
            lastUsed.putAll(data.lastUsed)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveToStorage() {
        try {
            val data = EmojiUsageData(
                usageCount = usageCount.toMap(),
                lastUsed = lastUsed.toMap()
            )
            val jsonData = RevoltJson.encodeToString(data)
            
            val prefs = RevoltApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_DATA, jsonData)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}