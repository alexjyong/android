package chat.revolt.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import chat.revolt.persistence.KVStorage
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.logcat

object NotificationPollingService {
    private const val KEY_POLLING_ENABLED = "notification_polling_enabled"
    private const val KEY_POLLING_INTERVAL = "notification_polling_interval_minutes"
    private const val DEFAULT_INTERVAL_MINUTES = 15L

    fun start(context: Context, intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES) {
        logcat(LogPriority.INFO) { "NotificationPollingService: Starting with interval $intervalMinutes minutes" }

        val kvStorage = KVStorage(context)
        runBlocking {
            kvStorage.set(KEY_POLLING_ENABLED, true)
            kvStorage.set(KEY_POLLING_INTERVAL, intervalMinutes.toString())
        }

        scheduleNextPoll(context)
    }

    fun stop(context: Context) {
        logcat(LogPriority.INFO) { "NotificationPollingService: Stopping" }

        val kvStorage = KVStorage(context)
        runBlocking {
            kvStorage.set(KEY_POLLING_ENABLED, false)
        }

        cancelScheduledPoll(context)
    }

    fun scheduleNextPoll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val kvStorage = KVStorage(context)

        val intervalMinutes = runBlocking {
            kvStorage.get(KEY_POLLING_INTERVAL)?.toLongOrNull() ?: DEFAULT_INTERVAL_MINUTES
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
        val kvStorage = KVStorage(context)
        val isEnabled = runBlocking {
            kvStorage.getBoolean(KEY_POLLING_ENABLED) ?: false
        }

        if (isEnabled) {
            logcat(LogPriority.INFO) { "NotificationPollingService: Rescheduling after boot" }
            scheduleNextPoll(context)
        }
    }

    fun isPollingEnabled(context: Context): Boolean {
        val kvStorage = KVStorage(context)
        return runBlocking {
            kvStorage.getBoolean(KEY_POLLING_ENABLED) ?: false
        }
    }

    fun getPollingInterval(context: Context): Long {
        val kvStorage = KVStorage(context)
        return runBlocking {
            kvStorage.get(KEY_POLLING_INTERVAL)?.toLongOrNull() ?: DEFAULT_INTERVAL_MINUTES
        }
    }

    fun updateInterval(context: Context, intervalMinutes: Long) {
        val kvStorage = KVStorage(context)
        runBlocking {
            kvStorage.set(KEY_POLLING_INTERVAL, intervalMinutes.toString())
        }

        if (isPollingEnabled(context)) {
            cancelScheduledPoll(context)
            scheduleNextPoll(context)
            logcat(LogPriority.INFO) { "NotificationPollingService: Updated interval to $intervalMinutes minutes" }
        }
    }
}