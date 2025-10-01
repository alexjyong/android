package chat.revolt.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat

class NotificationPollingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_POLL_NOTIFICATIONS -> {
                logcat(LogPriority.DEBUG) { "NotificationPollingReceiver: Alarm triggered" }

                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        NotificationPollingWorker.checkForNewMessages(context)
                        NotificationPollingService.scheduleNextPoll(context)
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR) { "NotificationPollingReceiver: Error during poll: ${e.message}" }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON" -> {
                logcat(LogPriority.INFO) { "NotificationPollingReceiver: Device booted, rescheduling polling" }
                NotificationPollingService.rescheduleAfterBoot(context)
            }
        }
    }

    companion object {
        const val ACTION_POLL_NOTIFICATIONS = "chat.revolt.POLL_NOTIFICATIONS"
    }
}