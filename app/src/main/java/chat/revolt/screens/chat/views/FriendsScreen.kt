package chat.revolt.screens.chat.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.internals.FriendRequests
import chat.revolt.api.routes.user.unfriendUser
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.composables.chat.MemberListItem
import chat.revolt.composables.generic.CountableListHeader
import chat.revolt.internals.extensions.zero
import chat.revolt.screens.chat.LocalIsConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(topNav: NavController, useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    var overflowMenuShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                AnimatedVisibility(LocalIsConnected.current) {
                    Spacer(
                        Modifier
                            .height(
                                WindowInsets.statusBars.asPaddingValues()
                                    .calculateTopPadding()
                            )
                    )
                }
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.friends),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (useDrawer) {
                            IconButton(onClick = {
                                onDrawerClicked()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            topNav.navigate("create/group")
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_account_multiple_plus_24dp),
                                contentDescription = stringResource(R.string.frends_new_group)
                            )
                        }
                        IconButton(onClick = {
                            overflowMenuShown = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                        DropdownMenu(
                            expanded = overflowMenuShown,
                            onDismissRequest = {
                                overflowMenuShown = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.friends_deny_all_incoming))
                                },
                                onClick = {
                                    scope.launch {
                                        overflowMenuShown = false
                                    }
                                    with(Dispatchers.IO) {
                                        scope.launch {
                                            FriendRequests.getIncoming()
                                                .forEach { it.id?.let { id -> unfriendUser(id) } }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    windowInsets = WindowInsets.zero
                )
            }
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxHeight()
        ) {
            LazyColumn {
                stickyHeader(key = "incoming") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_incoming_requests),
                        count = FriendRequests.getIncoming().size
                    )
                }

                items(FriendRequests.getIncoming().size) {
                    val item = FriendRequests.getIncoming().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "outgoing") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_outgoing_requests),
                        count = FriendRequests.getOutgoing().size
                    )
                }

                items(FriendRequests.getOutgoing().size) {
                    val item = FriendRequests.getOutgoing().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "online") {
                    CountableListHeader(
                        text = stringResource(id = R.string.status_online),
                        count = FriendRequests.getOnlineFriends().size
                    )
                }

                items(FriendRequests.getOnlineFriends().size) {
                    val item = FriendRequests.getOnlineFriends().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "not_online") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_all),
                        count = FriendRequests.getFriends(true).size
                    )
                }

                items(FriendRequests.getFriends(true).size) {
                    val item = FriendRequests.getFriends(true).getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "blocked") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_blocked),
                        count = FriendRequests.getBlocked().size
                    )
                }


                items(FriendRequests.getBlocked().size) {
                    val item = FriendRequests.getBlocked().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}