package chat.revolt.services

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import androidx.core.content.ContextCompat
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
    companion object {
        private const val KEY_BACKGROUND_SERVICE_ENABLED = "notification_background_service_enabled"
    }

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
            val isBackgroundServiceEnabled = kvStorage.getBoolean(KEY_BACKGROUND_SERVICE_ENABLED) ?: false

            if (isBackgroundServiceEnabled && hasNotificationPermission()) {
                NotificationForegroundService.start(context)
            }
        }
    }
}