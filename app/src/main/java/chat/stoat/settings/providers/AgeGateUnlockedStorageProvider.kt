package chat.stoat.settings.providers

import chat.stoat.StoatApplication
import chat.stoat.persistence.KVStorage

object AgeGateUnlockedStorageProvider {
    private val kv = KVStorage(StoatApplication.instance)

    suspend fun setAgeGateUnlocked(unlocked: Boolean) {
        kv.set("ageGateUnlocked", unlocked)
    }

    suspend fun getAgeGateUnlocked(): Boolean {
        return kv.getBoolean("ageGateUnlocked") ?: false
    }
}