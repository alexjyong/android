package chat.revolt.internals.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.Roles

data class ChannelPermissionState(
    val permissions: Long,
    val isLoading: Boolean
)

@Composable
fun rememberChannelPermissions(channelId: String, key1: Any = Unit): MutableLongState {
    val permissions = rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(channelId, key1) {
        if (RevoltAPI.selfId == null) return@LaunchedEffect
        if (RevoltAPI.userCache[RevoltAPI.selfId] == null) return@LaunchedEffect
        if (RevoltAPI.channelCache[channelId] == null) return@LaunchedEffect

        val channel = RevoltAPI.channelCache[channelId]
        val selfUser = RevoltAPI.userCache[RevoltAPI.selfId]
        val member = channel?.let {
            it.server?.let { server ->
                RevoltAPI.selfId?.let { selfId ->
                    RevoltAPI.members.getMember(server, selfId)
                }
            }
        }
        channel?.let { permissions.longValue = Roles.permissionFor(it, selfUser, member) }
    }

    return permissions
}

@Composable
fun rememberChannelPermissionsWithLoading(channelId: String, key1: Any = Unit): ChannelPermissionState {
    val permissions = rememberSaveable { mutableLongStateOf(0L) }
    val isLoading = rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(channelId, key1) {
        isLoading.value = true
        
        if (RevoltAPI.selfId == null) return@LaunchedEffect
        if (RevoltAPI.userCache[RevoltAPI.selfId] == null) return@LaunchedEffect
        if (RevoltAPI.channelCache[channelId] == null) return@LaunchedEffect

        val channel = RevoltAPI.channelCache[channelId]
        val selfUser = RevoltAPI.userCache[RevoltAPI.selfId]
        val member = channel?.let {
            it.server?.let { server ->
                RevoltAPI.selfId?.let { selfId ->
                    RevoltAPI.members.getMember(server, selfId)
                }
            }
        }
        
        channel?.let { 
            permissions.longValue = Roles.permissionFor(it, selfUser, member)
            isLoading.value = false
        }
    }

    return ChannelPermissionState(permissions.longValue, isLoading.value)
}