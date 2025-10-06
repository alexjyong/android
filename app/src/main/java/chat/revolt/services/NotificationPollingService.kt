package chat.revolt.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import chat.revolt.api.settings.NotificationSettingsProvider
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.logcat

object NotificationPollingService {

    fun start(context: Context, intervalMinutes: Long) {
        logcat(LogPriority.INFO) { "NotificationPollingService: Starting with interval $intervalMinutes minutes" }
        scheduleNextPoll(context)
    }

    fun stop(context: Context) {
        logcat(LogPriority.INFO) { "NotificationPollingService: Stopping" }
        cancelScheduledPoll(context)
    }

    fun scheduleNextPoll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intervalMinutes = runBlocking {
            NotificationSettingsProvider.getPollingInterval()
        }

        val triggerAtMillis = System.currentTimeMillis() + (intervalMinutes * 60 * 1000)

        val intent = Intent(context, NotificationPollingReceiver::class.java).apply {
            action = NotificationPollingReceiver.ACTION_POLL_NOTIFICATIONS
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                        logcat(LogPriority.DEBUG) { "NotificationPollingService: Scheduled exact alarm for ${intervalMinutes}min" }
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                        logcat(LogPriority.WARN) { "NotificationPollingService: Exact alarm permission denied, using inexact alarm" }
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    logcat(LogPriority.DEBUG) { "NotificationPollingService: Scheduled exact alarm for ${intervalMinutes}min" }
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                logcat(LogPriority.DEBUG) { "NotificationPollingService: Scheduled exact alarm for ${intervalMinutes}min" }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "NotificationPollingService: Failed to schedule alarm: ${e.message}" }
        }
    }

    fun cancelScheduledPoll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPollingReceiver::class.java).apply {
            action = NotificationPollingReceiver.ACTION_POLL_NOTIFICATIONS
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        logcat(LogPriority.DEBUG) { "NotificationPollingService: Cancelled scheduled poll" }
    }

    fun rescheduleAfterBoot(context: Context) {
        val mode = runBlocking {
            NotificationSettingsProvider.getNotificationMode()
        }

        if (mode == NotificationMode.BATTERY_SAVER) {
            logcat(LogPriority.INFO) { "NotificationPollingService: Rescheduling after boot" }
            scheduleNextPoll(context)
        }
    }
}