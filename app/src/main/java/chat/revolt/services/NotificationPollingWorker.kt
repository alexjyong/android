package chat.revolt.services

import android.content.Context
import chat.revolt.api.RevoltAPI
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.routes.channel.fetchMessagesFromChannel
import chat.revolt.api.routes.sync.syncUnreads
import chat.revolt.persistence.KVStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat

object NotificationPollingWorker {
    private const val KEY_LAST_UNREAD_STATE = "notification_polling_last_state"

    @Serializable
    data class UnreadState(
        val channelId: String,
        val lastMessageId: String,
        val mentions: List<String> = emptyList()
    )

    suspend fun checkForNewMessages(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                logcat(LogPriority.DEBUG) { "NotificationPollingWorker: Checking for new messages" }

                val currentUnreads = syncUnreads()
                val kvStorage = KVStorage(context)

                val lastStateJson = kvStorage.get(KEY_LAST_UNREAD_STATE)
                val lastState = if (lastStateJson != null) {
                    try {
                        Json.decodeFromString<List<UnreadState>>(lastStateJson)
                    } catch (e: Exception) {
                        logcat(LogPriority.WARN) { "Failed to parse last unread state: ${e.message}" }
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                val lastStateMap = lastState.associateBy { it.channelId }
                val notificationHelper = NotificationHelper(context)

                currentUnreads.forEach { unread ->
                    val channelId = unread.id.channel
                    val currentLastId = unread.last_id ?: return@forEach
                    val channel = RevoltAPI.channelCache[channelId]
                    val serverId = channel?.server

                    if (channel == null) {
                        logcat(LogPriority.DEBUG) { "Channel $channelId not found in cache" }
                        return@forEach
                    }

                    if (channel.type == "SavedMessages") {
                        return@forEach
                    }

                    val lastKnownState = lastStateMap[channelId]
                    val lastKnownId = lastKnownState?.lastMessageId

                    val hasNewMessages = if (lastKnownId != null) {
                        currentLastId > lastKnownId
                    } else {
                        true
                    }

                    if (hasNewMessages) {
                        try {
                            val messagesResult = fetchMessagesFromChannel(
                                channelId = channelId,
                                after = lastKnownId,
                                limit = 20
                            )

                            var notifiedForChannel = false
                            val messages = messagesResult.messages ?: emptyList()

                            for (message in messages) {
                                val filterResult = NotificationMessageFilter.shouldNotifyMessage(message, channel)

                                if (filterResult.shouldNotify) {
                                    val messageFrame = MessageFrame(
                                        id = message.id ?: continue,
                                        nonce = message.nonce,
                                        channel = channelId,
                                        author = message.author ?: continue,
                                        content = message.content,
                                        system = null,
                                        attachments = message.attachments,
                                        edited = message.edited,
                                        embeds = message.embeds,
                                        mentions = message.mentions,
                                        replies = message.replies,
                                        masquerade = message.masquerade
                                    )

                                    val author = RevoltAPI.userCache[message.author]
                                    val server = serverId?.let { RevoltAPI.serverCache[it] }

                                    notificationHelper.showMessageNotification(
                                        messageFrame = messageFrame,
                                        author = author,
                                        channel = channel,
                                        server = server
                                    )

                                    notifiedForChannel = true
                                    logcat(LogPriority.INFO) { "NotificationPollingWorker: Showed notification for message ${message.id}" }
                                }
                            }

                            if (!notifiedForChannel) {
                                logcat(LogPriority.DEBUG) { "NotificationPollingWorker: No messages passed filters in channel $channelId" }
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) { "NotificationPollingWorker: Error fetching messages for channel $channelId: ${e.message}" }
                        }
                    }
                }

                val newState = currentUnreads.mapNotNull { unread ->
                    val lastId = unread.last_id ?: return@mapNotNull null
                    UnreadState(
                        channelId = unread.id.channel,
                        lastMessageId = lastId,
                        mentions = unread.mentions ?: emptyList()
                    )
                }

                kvStorage.set(KEY_LAST_UNREAD_STATE, Json.encodeToString(newState))

                logcat(LogPriority.DEBUG) { "NotificationPollingWorker: Check completed" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "NotificationPollingWorker: Error checking messages: ${e.message}" }
            }
        }
    }

    suspend fun clearState(context: Context) {
        val kvStorage = KVStorage(context)
        kvStorage.set(KEY_LAST_UNREAD_STATE, "")
    }
}