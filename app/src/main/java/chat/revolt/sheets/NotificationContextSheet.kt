package chat.revolt.sheets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.NotificationState
import chat.revolt.api.settings.NotificationSettingsProvider
import chat.revolt.composables.generic.SheetButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ColumnScope.ChannelNotificationContextSheet(
    channelId: String,
    serverId: String? = null,
    dismissSheet: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val channel = RevoltAPI.channelCache[channelId]

    if (channel == null) return

    var showMuteOptions by remember { mutableStateOf(false) }

    val isChannelMuted = NotificationSettingsProvider.isChannelMuted(channelId)
    val currentChannelState = NotificationSettingsProvider.getChannelNotificationState(channelId, serverId)
    val channelMute = NotificationSettingsProvider.getChannelMute(channelId)
    val serverState = if (serverId != null) {
        NotificationSettingsProvider.getServerNotificationState(serverId)
    } else null

    if (showMuteOptions) {
        val muteOptions = listOf(
            15 * 60 * 1000L to stringResource(R.string.mute_for_15_minutes),
            60 * 60 * 1000L to stringResource(R.string.mute_for_1_hour),
            3 * 60 * 60 * 1000L to stringResource(R.string.mute_for_3_hours),
            8 * 60 * 60 * 1000L to stringResource(R.string.mute_for_8_hours),
            24 * 60 * 60 * 1000L to stringResource(R.string.mute_for_24_hours),
            null to stringResource(R.string.mute_until_turned_off)
        )

        muteOptions.forEach { (durationMs, label) ->
            SheetButton(
                headlineContent = { Text(label) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.icn_close_24dp),
                        contentDescription = null
                    )
                },
                onClick = {
                    scope.launch {
                        val until = if (durationMs != null) {
                            System.currentTimeMillis() + durationMs
                        } else null
                        NotificationSettingsProvider.muteChannel(channelId, until)
                        dismissSheet()
                    }
                }
            )
        }

        SheetButton(
            headlineContent = { Text("Back") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.icn_arrow_back_24dp),
                    contentDescription = null
                )
            },
            onClick = { showMuteOptions = false }
        )
        return
    }

    if (isChannelMuted) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(R.string.unmute_channel),
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = channelMute?.until?.let { until ->
                {
                    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    Text(
                        text = stringResource(R.string.muted_until, formatter.format(Date(until))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.icn_notification_settings_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                scope.launch {
                    NotificationSettingsProvider.unmuteChannel(channelId)
                    dismissSheet()
                }
            }
        )
    } else {
        SheetButton(
            headlineContent = { Text(stringResource(R.string.mute_channel)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.icn_close_24dp),
                    contentDescription = null
                )
            },
            onClick = { showMuteOptions = true }
        )
    }

    Text(
        text = stringResource(R.string.notification_menu_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    val serverDefaultLabel = if (serverId != null) {
        stringResource(R.string.notification_state_server_default)
    } else {
        stringResource(R.string.notification_state_default)
    }

    val defaultStateDescription = when {
        serverId != null && serverState != null -> {
            when (serverState) {
                NotificationState.ALL -> stringResource(R.string.notification_state_all)
                NotificationState.MENTION -> stringResource(R.string.notification_state_mention)
                NotificationState.NONE -> stringResource(R.string.notification_state_none)
            }
        }
        serverId == null -> {
            when (channel?.type) {
                "DirectMessage", "Group", "SavedMessages" -> stringResource(R.string.notification_state_all)
                else -> stringResource(R.string.notification_state_mention)
            }
        }
        else -> stringResource(R.string.notification_state_mention)
    }

    val isUsingDefault = NotificationSettingsProvider.getChannelNotificationState(channelId, serverId).let { state ->
        val explicitSetting = chat.revolt.api.settings.SyncedSettings.notifications.channel[channelId]
        explicitSetting == null
    }

    SheetButton(
        headlineContent = { Text(serverDefaultLabel) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_notification_settings_24dp),
                contentDescription = null
            )
        },
        supportingContent = {
            Text(
                text = defaultStateDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = if (isUsingDefault) {
            {
                Icon(
                    painter = painterResource(R.drawable.icn_check_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        onClick = {
            scope.launch {
                NotificationSettingsProvider.setChannelNotificationState(channelId, null)
                dismissSheet()
            }
        }
    )

    SheetButton(
        headlineContent = { Text(stringResource(R.string.notification_state_all)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_notification_settings_24dp),
                contentDescription = null
            )
        },
        trailingContent = if (currentChannelState == NotificationState.ALL && !isUsingDefault) {
            {
                Icon(
                    painter = painterResource(R.drawable.icn_check_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        onClick = {
            scope.launch {
                NotificationSettingsProvider.setChannelNotificationState(channelId, NotificationState.ALL)
                dismissSheet()
            }
        }
    )

    SheetButton(
        headlineContent = { Text(stringResource(R.string.notification_state_mention)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_campaign_24dp),
                contentDescription = null
            )
        },
        trailingContent = if (currentChannelState == NotificationState.MENTION && !isUsingDefault) {
            {
                Icon(
                    painter = painterResource(R.drawable.icn_check_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        onClick = {
            scope.launch {
                NotificationSettingsProvider.setChannelNotificationState(channelId, NotificationState.MENTION)
                dismissSheet()
            }
        }
    )

    SheetButton(
        headlineContent = { Text(stringResource(R.string.notification_state_none)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_close_24dp),
                contentDescription = null
            )
        },
        trailingContent = if (currentChannelState == NotificationState.NONE && !isUsingDefault) {
            {
                Icon(
                    painter = painterResource(R.drawable.icn_check_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        onClick = {
            scope.launch {
                NotificationSettingsProvider.setChannelNotificationState(channelId, NotificationState.NONE)
                dismissSheet()
            }
        }
    )
}

@Composable
fun ColumnScope.ServerNotificationContextSheet(
    serverId: String,
    dismissSheet: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val server = RevoltAPI.serverCache[serverId]

    if (server == null) return

    var showMuteOptions by remember { mutableStateOf(false) }

    val isServerMuted = NotificationSettingsProvider.isServerMuted(serverId)
    val currentServerState = NotificationSettingsProvider.getServerNotificationState(serverId)
    val serverMute = NotificationSettingsProvider.getServerMute(serverId)

    if (showMuteOptions) {
        val muteOptions = listOf(
            15 * 60 * 1000L to stringResource(R.string.mute_for_15_minutes),
            60 * 60 * 1000L to stringResource(R.string.mute_for_1_hour),
            3 * 60 * 60 * 1000L to stringResource(R.string.mute_for_3_hours),
            8 * 60 * 60 * 1000L to stringResource(R.string.mute_for_8_hours),
            24 * 60 * 60 * 1000L to stringResource(R.string.mute_for_24_hours),
            null to stringResource(R.string.mute_until_turned_off)
        )

        muteOptions.forEach { (durationMs, label) ->
            SheetButton(
                headlineContent = { Text(label) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.icn_close_24dp),
                        contentDescription = null
                    )
                },
                onClick = {
                    scope.launch {
                        val until = if (durationMs != null) {
                            System.currentTimeMillis() + durationMs
                        } else null
                        NotificationSettingsProvider.muteServer(serverId, until)
                        dismissSheet()
                    }
                }
            )
        }

        SheetButton(
            headlineContent = { Text("Back") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.icn_arrow_back_24dp),
                    contentDescription = null
                )
            },
            onClick = { showMuteOptions = false }
        )
        return
    }

    if (isServerMuted) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(R.string.unmute_server),
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = serverMute?.until?.let { until ->
                {
                    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    Text(
                        text = stringResource(R.string.muted_until, formatter.format(Date(until))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.icn_notification_settings_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                scope.launch {
                    NotificationSettingsProvider.unmuteServer(serverId)
                    dismissSheet()
                }
            }
        )
    } else {
        SheetButton(
            headlineContent = { Text(stringResource(R.string.mute_server)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.icn_close_24dp),
                    contentDescription = null
                )
            },
            onClick = { showMuteOptions = true }
        )
    }

    Text(
        text = stringResource(R.string.notification_menu_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    SheetButton(
        headlineContent = { Text(stringResource(R.string.notification_state_all)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_notification_settings_24dp),
                contentDescription = null
            )
        },
        trailingContent = if (currentServerState == NotificationState.ALL) {
            {
                Icon(
                    painter = painterResource(R.drawable.icn_check_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        onClick = {
            scope.launch {
                NotificationSettingsProvider.setServerNotificationState(serverId, NotificationState.ALL)
                dismissSheet()
            }
        }
    )

    SheetButton(
        headlineContent = { Text(stringResource(R.string.notification_state_mention)) },
        supportingContent = {
            Text(
                text = "Default for servers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_campaign_24dp),
                contentDescription = null
            )
        },
        trailingContent = if (currentServerState == NotificationState.MENTION) {
            {
                Icon(
                    painter = painterResource(R.drawable.icn_check_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        onClick = {
            scope.launch {
                NotificationSettingsProvider.setServerNotificationState(serverId, NotificationState.MENTION)
                dismissSheet()
            }
        }
    )

    SheetButton(
        headlineContent = { Text(stringResource(R.string.notification_state_none)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_close_24dp),
                contentDescription = null
            )
        },
        trailingContent = if (currentServerState == NotificationState.NONE) {
            {
                Icon(
                    painter = painterResource(R.drawable.icn_check_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        onClick = {
            scope.launch {
                NotificationSettingsProvider.setServerNotificationState(serverId, NotificationState.NONE)
                dismissSheet()
            }
        }
    )
}