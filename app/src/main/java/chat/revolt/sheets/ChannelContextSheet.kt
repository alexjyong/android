package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.PermissionBit
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.has
import chat.revolt.api.schemas.ChannelType
import chat.revolt.composables.generic.SheetButton
import chat.revolt.internals.Platform
import chat.revolt.screens.chat.dialogs.InviteDialog
import chat.revolt.sheets.ChannelNotificationContextSheet
import kotlinx.coroutines.launch

@Composable
fun ChannelContextSheet(channelId: String, onHideSheet: suspend () -> Unit) {
    val channel = RevoltAPI.channelCache[channelId]
    if (channel == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    var showNotificationSubmenu by remember { mutableStateOf(false) }
    var inviteDialogShown by remember { mutableStateOf(false) }

    if (showNotificationSubmenu) {
        Column {
            SheetButton(
                headlineContent = { Text("← Notifications") },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.icn_arrow_back_24dp),
                        contentDescription = null
                    )
                },
                onClick = { showNotificationSubmenu = false }
            )

            ChannelNotificationContextSheet(
                channelId = channelId,
                serverId = channel.server,
                dismissSheet = onHideSheet
            )
        }
        return
    }

    if (inviteDialogShown) {
        InviteDialog(
            channelId = channelId,
            onDismissRequest = { 
                inviteDialogShown = false
                coroutineScope.launch { onHideSheet() }
            }
        )
    }

    Column {
        SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.channel_context_sheet_actions_copy_id),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.icn_identifier_copy_24dp),
                contentDescription = null
            )
        },
        onClick = {
            if (channel.id == null) return@SheetButton

            clipboardManager.setText(AnnotatedString(channel.id))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.channel_context_sheet_actions_copy_id_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }

            coroutineScope.launch {
                onHideSheet()
            }
        }
    )

    if (
        (channel.channelType == ChannelType.TextChannel || channel.channelType == ChannelType.VoiceChannel) &&
        Roles.permissionFor(channel, RevoltAPI.userCache[RevoltAPI.selfId]) has PermissionBit.InviteOthers
    ) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.channel_info_sheet_options_invite),
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.icn_add_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                inviteDialogShown = true
            }
        )
    }

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.channel_context_sheet_actions_mark_read),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.icn_mark_chat_read_24dp),
                contentDescription = null
            )
        },
        onClick = {
            coroutineScope.launch {
                channel.lastMessageID?.let {
                    RevoltAPI.unreads.markAsRead(channelId, it, sync = true)
                }
                onHideSheet()
            }
        }
    )

    SheetButton(
        headlineContent = { Text(stringResource(R.string.notification_menu_title)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_notification_settings_24dp),
                contentDescription = null
            )
        },
        onClick = { showNotificationSubmenu = true }
    )
    }
}
