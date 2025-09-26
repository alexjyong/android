package chat.revolt.api.internals

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton to track the currently active channel and app state for smart notification filtering
 */
object CurrentChannelState {
    private val _currentChannelId = MutableStateFlow<String?>(null)
    val currentChannelId: StateFlow<String?> = _currentChannelId.asStateFlow()

    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    fun setCurrentChannel(channelId: String?) {
        _currentChannelId.value = channelId
    }

    fun getCurrentChannel(): String? = _currentChannelId.value

    fun setAppForegroundState(isInForeground: Boolean) {
        _isAppInForeground.value = isInForeground
        // Clear current channel when app goes to background
        if (!isInForeground) {
            _currentChannelId.value = null
        }
    }

    fun isAppInForeground(): Boolean = _isAppInForeground.value

    /**
     * Should filter notifications only if app is in foreground AND user is viewing the specific channel
     */
    fun shouldFilterNotification(channelId: String): Boolean {
        return isAppInForeground() && getCurrentChannel() == channelId
    }
}