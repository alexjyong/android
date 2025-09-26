package chat.revolt.api.settings

import chat.revolt.api.schemas.NotificationState
import chat.revolt.api.schemas.MuteState
import chat.revolt.persistence.Database
import chat.revolt.persistence.SqlStorage
import chat.revolt.persistence.KVStorage
import chat.revolt.RevoltApplication

object DefaultNotificationStates {
    const val SAVED_MESSAGES = "all"
    const val DIRECT_MESSAGE = "all"
    const val GROUP = "all"
    const val TEXT_CHANNEL = "mention"
    const val VOICE_CHANNEL = "mention"
    const val SERVER_DEFAULT = "mention"
}

object NotificationSettingsProvider {
    private val kv = KVStorage(RevoltApplication.instance)

    private fun getCurrentTime() = System.currentTimeMillis()
    
    suspend fun getServerSuppressEveryoneMentions(serverId: String): Boolean {
        return kv.getBoolean("suppress_everyone_server_$serverId") ?: false
    }
    
    suspend fun setServerSuppressEveryoneMentions(serverId: String, suppress: Boolean) {
        kv.set("suppress_everyone_server_$serverId", suppress)
    }
    
    suspend fun getChannelSuppressEveryoneMentions(channelId: String): Boolean {
        return kv.getBoolean("suppress_everyone_channel_$channelId") ?: false
    }
    
    suspend fun setChannelSuppressEveryoneMentions(channelId: String, suppress: Boolean) {
        kv.set("suppress_everyone_channel_$channelId", suppress)
    }
    
    suspend fun shouldSuppressEveryoneMentions(channelId: String, serverId: String?): Boolean {
        val channelSuppressed = getChannelSuppressEveryoneMentions(channelId)
        
        if (channelSuppressed) {
            return true
        }
        
        if (serverId != null) {
            val serverSuppressed = getServerSuppressEveryoneMentions(serverId)
            if (serverSuppressed) {
                return true
            }
        }
        
        return false
    }

    fun isServerMuted(serverId: String): Boolean {
        val muteState = SyncedSettings.notifications.server_mutes[serverId] ?: return false
        return muteState.until == null || muteState.until > getCurrentTime()
    }

    fun isChannelMuted(channelId: String): Boolean {
        val muteState = SyncedSettings.notifications.channel_mutes[channelId] ?: return false
        return muteState.until == null || muteState.until > getCurrentTime()
    }

    fun isChannelMuted(channelId: String, serverId: String?): Boolean {
        if (serverId != null && isServerMuted(serverId)) return true
        return isChannelMuted(channelId)
    }

    fun getServerNotificationState(serverId: String): NotificationState {
        val setting = SyncedSettings.notifications.server[serverId]
        return NotificationState.fromString(setting) ?: NotificationState.MENTION
    }

    fun getChannelNotificationState(channelId: String, serverId: String? = null): NotificationState {
        val channelSetting = SyncedSettings.notifications.channel[channelId]
        if (channelSetting != null) {
            return NotificationState.fromString(channelSetting) ?: getDefaultForChannel(channelId)
        }

        if (serverId != null) {
            return getServerNotificationState(serverId)
        }
        return getDefaultForChannel(channelId)
    }

    private fun getDefaultForChannel(channelId: String): NotificationState {
        return try {
            val db = Database(SqlStorage.driver)
            val channel = db.channelQueries.findById(channelId).executeAsOneOrNull()

            when (channel?.channelType) {
                "SavedMessages" -> NotificationState.ALL
                "DirectMessage" -> NotificationState.ALL
                "Group" -> NotificationState.ALL
                "TextChannel", "VoiceChannel" -> NotificationState.MENTION
                else -> NotificationState.MENTION
            }
        } catch (e: Exception) {
            NotificationState.MENTION
        }
    }

    fun shouldNotify(channelId: String, serverId: String?, isMention: Boolean): Boolean {
        if (isChannelMuted(channelId, serverId)) return false
        val state = getChannelNotificationState(channelId, serverId)

        return when (state) {
            NotificationState.ALL -> true
            NotificationState.MENTION -> isMention
            NotificationState.NONE -> false
        }
    }

    suspend fun setServerNotificationState(serverId: String, state: NotificationState?) {
        val current = SyncedSettings.notifications
        val newServerMap = current.server.toMutableMap()

        if (state != null) {
            newServerMap[serverId] = state.value
        } else {
            newServerMap.remove(serverId)
        }

        SyncedSettings.updateNotifications(
            current.copy(server = newServerMap)
        )
    }

    suspend fun setChannelNotificationState(channelId: String, state: NotificationState?) {
        val current = SyncedSettings.notifications
        val newChannelMap = current.channel.toMutableMap()

        if (state != null) {
            newChannelMap[channelId] = state.value
        } else {
            newChannelMap.remove(channelId)
        }

        SyncedSettings.updateNotifications(
            current.copy(channel = newChannelMap)
        )
    }

    suspend fun muteServer(serverId: String, until: Long? = null) {
        val current = SyncedSettings.notifications
        val newMutes = current.server_mutes.toMutableMap()
        newMutes[serverId] = MuteState(until)

        SyncedSettings.updateNotifications(
            current.copy(server_mutes = newMutes)
        )
    }

    suspend fun muteChannel(channelId: String, until: Long? = null) {
        val current = SyncedSettings.notifications
        val newMutes = current.channel_mutes.toMutableMap()
        newMutes[channelId] = MuteState(until)

        SyncedSettings.updateNotifications(
            current.copy(channel_mutes = newMutes)
        )
    }

    suspend fun unmuteServer(serverId: String) {
        val current = SyncedSettings.notifications
        val newMutes = current.server_mutes.toMutableMap()
        newMutes.remove(serverId)

        SyncedSettings.updateNotifications(
            current.copy(server_mutes = newMutes)
        )
    }

    suspend fun unmuteChannel(channelId: String) {
        val current = SyncedSettings.notifications
        val newMutes = current.channel_mutes.toMutableMap()
        newMutes.remove(channelId)

        SyncedSettings.updateNotifications(
            current.copy(channel_mutes = newMutes)
        )
    }

    fun getServerMute(serverId: String): MuteState? {
        return SyncedSettings.notifications.server_mutes[serverId]
    }

    fun getChannelMute(channelId: String): MuteState? {
        return SyncedSettings.notifications.channel_mutes[channelId]
    }
}