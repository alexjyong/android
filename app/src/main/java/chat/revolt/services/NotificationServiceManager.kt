package chat.revolt.services

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import androidx.core.content.ContextCompat
import chat.revolt.api.settings.NotificationSettingsProvider
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kvStorage: KVStorage
) {

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startServicesIfEnabled(applicationScope: CoroutineScope) {
        applicationScope.launch {
            val notificationMode = NotificationSettingsProvider.getNotificationMode()

            if (hasNotificationPermission()) {
                when (notificationMode) {
                    NotificationMode.INSTANT -> {
                        NotificationForegroundService.start(context)
                        NotificationPollingService.stop(context)
                    }
                    NotificationMode.BATTERY_SAVER -> {
                        NotificationForegroundService.stop(context)
                        val interval = NotificationSettingsProvider.getPollingInterval()
                        NotificationPollingService.start(context, interval)
                    }
                    NotificationMode.OFF -> {
                        NotificationForegroundService.stop(context)
                        NotificationPollingService.stop(context)
                    }
                }
            }
        }
    }
}