package chat.revolt.services

import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.CurrentChannelState
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Message
import chat.revolt.api.settings.NotificationSettingsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat

data class MessageFilterResult(
    val shouldNotify: Boolean,
    val isMention: Boolean
)

object NotificationMessageFilter {
    fun shouldNotifyForChannel(channelId: String, serverId: String?, isMention: Boolean): Boolean {
        return try {
            val channel = RevoltAPI.channelCache[channelId]

            when {
                channel == null -> false
                channel.type == "SavedMessages" -> false
                CurrentChannelState.shouldFilterNotification(channelId) -> {
                    logcat(LogPriority.DEBUG) { "Notification filtered: message is for currently active channel $channelId (app in foreground)" }
                    false
                }
                else -> NotificationSettingsProvider.shouldNotify(channelId, serverId, isMention)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Notification logic failed: ${e.message}" }
            isMention
        }
    }

    suspend fun shouldNotifyMessage(
        message: Message,
        channel: Channel?
    ): MessageFilterResult = withContext(Dispatchers.Main) {
        val channelId = message.channel ?: return@withContext MessageFilterResult(false, false)
        val serverId = channel?.server

        if (message.author == RevoltAPI.selfId) {
            return@withContext MessageFilterResult(false, false)
        }

        val selfId = RevoltAPI.selfId ?: return@withContext MessageFilterResult(false, false)

        val suppressEveryoneMentions = NotificationSettingsProvider.shouldSuppressEveryoneMentions(channelId, serverId)
        val containsEveryone = message.content?.contains("@everyone") == true
        val containsHere = message.content?.contains("@here") == true

        if (suppressEveryoneMentions && (containsEveryone || containsHere)) {
            return@withContext MessageFilterResult(false, false)
        }

        val hasDirectMention = message.mentions?.contains(selfId) == true
        val hasMassMention = containsEveryone || containsHere

        var hasRoleMention = false
        if (serverId != null && message.content != null) {
            val mentionedRoleIds = chat.revolt.internals.text.MessageProcessor.findMentionedRoleIDs(message.content)
            if (mentionedRoleIds.isNotEmpty()) {
                val member = RevoltAPI.members.getMember(serverId, selfId)
                val userRoles = member?.roles ?: emptyList()
                hasRoleMention = mentionedRoleIds.any { roleId -> userRoles.contains(roleId) }
            }
        }

        val isMention = hasDirectMention || hasMassMention || hasRoleMention

        val shouldNotify = shouldNotifyForChannel(channelId, serverId, isMention)

        MessageFilterResult(shouldNotify, isMention)
    }
}
