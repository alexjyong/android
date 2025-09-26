package chat.revolt.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import chat.revolt.R
import chat.revolt.activities.MainActivity
import chat.revolt.api.REVOLT_WEBSOCKET
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.realtime.frames.receivable.AnyFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.realtime.frames.sendable.AuthorizationFrame
import chat.revolt.api.schemas.NotificationState
import chat.revolt.api.settings.NotificationSettingsProvider
import chat.revolt.api.internals.CurrentChannelState
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat

class NotificationForegroundService : Service() {
    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "revolt_notification_service"
        const val ACTION_START_SERVICE = "START_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var websocketJob: Job? = null
    private lateinit var notificationHelper: NotificationHelper

    private fun shouldNotifyMessage(channelId: String, serverId: String?, isMention: Boolean): Boolean {
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

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        createNotificationChannel()
        logcat(LogPriority.INFO) { "NotificationForegroundService created" }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
                return START_STICKY
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createForegroundNotification()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        logcat(LogPriority.INFO) { "Starting WebSocket connection for notifications" }
        startWebSocketConnection()
    }

    private fun startWebSocketConnection() {
        websocketJob?.cancel()

        websocketJob = serviceScope.launch {
            var retryCount = 0

            while (true) {
                try {
                    logcat(LogPriority.INFO) { "Connecting to WebSocket..." }

                    RevoltHttp.ws(REVOLT_WEBSOCKET) {
                        logcat(LogPriority.INFO) { "WebSocket connected successfully" }
                        retryCount = 0

                        val authFrame = AuthorizationFrame("Authenticate", RevoltAPI.sessionToken)
                        send(RevoltJson.encodeToString(AuthorizationFrame.serializer(), authFrame))
                        logcat(LogPriority.DEBUG) { "Sent authentication frame" }

                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                handleWebSocketFrame(frame.readText())
                            }
                        }
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR) { "WebSocket error: ${e.message}" }
                    retryCount++

                    val retryDelay = minOf(30000, 1000 * (1 shl retryCount))
                    logcat(LogPriority.INFO) { "Retrying WebSocket connection in ${retryDelay}ms (attempt $retryCount)" }
                    delay(retryDelay.toLong())
                }
            }
        }
    }

    private suspend fun handleWebSocketFrame(frameString: String) {
        try {
            val frameType = RevoltJson.decodeFromString(AnyFrame.serializer(), frameString).type

            when (frameType) {
                "Message" -> {
                    val messageFrame = RevoltJson.decodeFromString(MessageFrame.serializer(), frameString)
                    handleNewMessage(messageFrame)
                }
                "Ready" -> {
                    logcat(LogPriority.INFO) { "WebSocket authenticated and ready" }
                }
                "Pong" -> {
                    logcat(LogPriority.DEBUG) { "Received pong frame" }
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Failed to handle WebSocket frame: ${e.message}" }
        }
    }

    private suspend fun handleNewMessage(messageFrame: MessageFrame) {
        withContext(Dispatchers.Main) {
            try {
                val channelId = messageFrame.channel ?: return@withContext
                val channel = RevoltAPI.channelCache[channelId]
                val serverId = channel?.server

                if (messageFrame.author == RevoltAPI.selfId) {
                    return@withContext
                }

                val selfId = RevoltAPI.selfId ?: return@withContext
                val isMention = messageFrame.mentions?.contains(selfId) == true ||
                        messageFrame.content?.contains("@everyone") == true ||
                        messageFrame.content?.contains("@here") == true

                if (shouldNotifyMessage(channelId, serverId, isMention)) {
                    val author = RevoltAPI.userCache[messageFrame.author]
                    val server = serverId?.let { RevoltAPI.serverCache[it] }

                    notificationHelper.showMessageNotification(
                        messageFrame = messageFrame,
                        author = author,
                        channel = channel,
                        server = server
                    )

                    logcat(LogPriority.DEBUG) { "Showed notification for message ${messageFrame.id}" }
                } else {
                    logcat(LogPriority.DEBUG) { "Notification filtered out for channel $channelId" }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Error handling message notification: ${e.message}" }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Revolt Notification Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Revolt connected for instant notifications"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Revolt Notifications")
            .setContentText("Connected and listening for messages")
            .setSmallIcon(R.drawable.ic_notification_monochrome)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        websocketJob?.cancel()
        logcat(LogPriority.INFO) { "NotificationForegroundService destroyed" }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}