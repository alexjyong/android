package chat.revolt.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import chat.revolt.R
import chat.revolt.activities.MainActivity
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Server
import chat.revolt.api.schemas.User
import logcat.LogPriority
import logcat.logcat

class NotificationHelper(private val context: Context) {
    companion object {
        const val MESSAGES_CHANNEL_ID = "revolt_messages"
        private const val MESSAGE_NOTIFICATION_ID_BASE = 2000
    }

    init {
        createNotificationChannels()
    }

    fun showMessageNotification(
        messageFrame: MessageFrame,
        author: User?,
        channel: Channel?,
        server: Server?
    ) {
        val channelName = when {
            server != null && channel != null -> "#${channel.name} in ${server.name}"
            channel?.name != null -> channel.name
            else -> "Direct Message"
        }

        val authorName = author?.displayName ?: author?.username ?: "Unknown User"
        val messageContent = messageFrame.content?.take(100) ?: ""

        val title = "$authorName in $channelName"
        val content = if (messageContent.isBlank()) "Sent an attachment" else messageContent

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            logcat(LogPriority.DEBUG) { "Creating notification intent for channel: ${messageFrame.channel}, messageId: ${messageFrame.id}" }
            putExtra("channelId", messageFrame.channel)
            putExtra("messageId", messageFrame.id)

            if (server != null && channel != null) {
                putExtra("serverId", server.id)
                putExtra("serverName", server.name)
                putExtra("channelName", channel.name)
                logcat(LogPriority.DEBUG) { "Added server context - serverId: ${server.id}, channelName: ${channel.name}" }
            }
        }

        val requestCode = (messageFrame.channel?.hashCode() ?: 0) + System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_monochrome)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
                    .setSummaryText(channelName)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val notificationId = MESSAGE_NOTIFICATION_ID_BASE + messageFrame.channel.hashCode()

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
        } catch (e: SecurityException) {
            logcat(LogPriority.ERROR) { "Failed to show notification: Missing POST_NOTIFICATIONS permission" }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messagesChannel = NotificationChannel(
                MESSAGES_CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New messages from channels and direct messages"
                enableVibration(true)
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(messagesChannel)
        }
    }
}