package chat.revolt.services

import android.content.Context
import chat.revolt.api.RevoltAPI
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.routes.channel.fetchMessagesFromChannel
import chat.revolt.api.routes.sync.syncUnreads
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.persistence.KVStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat

object NotificationPollingWorker {
    private const val KEY_LAST_SEEN_MESSAGES = "notification_polling_last_seen"

    @Serializable
    data class LastSeenMessages(
        val channelToMessageId: Map<String, String> = emptyMap()
    )

    suspend fun checkForNewMessages(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                SyncedSettings.fetch()

                logcat(LogPriority.DEBUG) { "NotificationPollingWorker: Checking for new messages" }

                val unreads = syncUnreads()
                val kvStorage = KVStorage(context)

                val lastSeenJson = kvStorage.get(KEY_LAST_SEEN_MESSAGES)
                val lastSeen = if (lastSeenJson != null) {
                    try {
                        Json.decodeFromString<LastSeenMessages>(lastSeenJson)
                    } catch (e: Exception) {
                        logcat(LogPriority.WARN) { "Failed to parse last seen messages: ${e.message}" }
                        LastSeenMessages()
                    }
                } else {
                    LastSeenMessages()
                }

                val updatedLastSeen = lastSeen.channelToMessageId.toMutableMap()
                val notificationHelper = NotificationHelper(context)

                unreads.forEach { unread ->
                    val channelId = unread.id.channel
                    val channel = RevoltAPI.channelCache[channelId]

                    if (channel == null) {
                        logcat(LogPriority.DEBUG) { "Channel $channelId not found in cache" }
                        return@forEach
                    }

                    if (channel.type == "SavedMessages") {
                        return@forEach
                    }

                    try {
                        val lastSeenId = updatedLastSeen[channelId]
                        val isFirstPollForChannel = lastSeenId == null

                        val messagesResult = fetchMessagesFromChannel(
                            channelId = channelId,
                            after = lastSeenId,
                            limit = if (isFirstPollForChannel) 1 else 20
                        )

                        val messages = messagesResult.messages ?: emptyList()

                        if (isFirstPollForChannel) {
                            logcat(LogPriority.DEBUG) { "First poll for channel $channelId, fetched ${messages.size} message(s)" }
                        }

                        messages.forEach { message ->
                            val messageId = message.id ?: return@forEach

                            val filterResult = NotificationMessageFilter.shouldNotifyMessage(message, channel)

                            if (filterResult.shouldNotify) {
                                val messageFrame = MessageFrame(
                                    id = messageId,
                                    nonce = message.nonce,
                                    channel = channelId,
                                    author = message.author ?: return@forEach,
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
                                val server = channel.server?.let { RevoltAPI.serverCache[it] }

                                notificationHelper.showMessageNotification(
                                    messageFrame = messageFrame,
                                    author = author,
                                    channel = channel,
                                    server = server
                                )

                                logcat(LogPriority.INFO) { "Showed notification for message $messageId in channel $channelId" }
                            } else {
                                logcat(LogPriority.DEBUG) { "Message $messageId filtered out" }
                            }
                        }

                        if (messages.isNotEmpty()) {
                            val newestMessageId = messages.maxByOrNull { it.id ?: "" }?.id
                            if (newestMessageId != null) {
                                updatedLastSeen[channelId] = newestMessageId
                            }
                        }
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR) { "Error processing channel $channelId: ${e.message}" }
                    }
                }

                kvStorage.set(KEY_LAST_SEEN_MESSAGES, Json.encodeToString(LastSeenMessages(updatedLastSeen)))

                logcat(LogPriority.DEBUG) { "NotificationPollingWorker: Check completed" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "NotificationPollingWorker: Error checking messages: ${e.message}" }
            }
        }
    }

    suspend fun clearState(context: Context) {
        val kvStorage = KVStorage(context)
        kvStorage.set(KEY_LAST_SEEN_MESSAGES, "")
    }
}
