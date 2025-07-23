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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun showInvalidClipboardToast(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.friends_add_by_tag_sheet_invalid_clipboard),
        Toast.LENGTH_SHORT
    ).show()
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun FriendsScreen(topNav: NavController, useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    var overflowMenuShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fabVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var addByTagSheetVisible by rememberSaveable { mutableStateOf(false) }
    var qrResult by rememberSaveable { mutableStateOf<QRResult?>(null) }

    val scanQrCodeLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
        qrResult = result
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    if (addByTagSheetVisible) {
        val addByTagSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var username by rememberSaveable { mutableStateOf("") }
        var tag by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = {
                addByTagSheetVisible = false
            },
            sheetGesturesEnabled = false,
            dragHandle = {},
            sheetState = addByTagSheetState,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    imageVector = RevoltTagIntro,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                )
                Text(
                    text = stringResource(R.string.friends_add_by_tag),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = AnnotatedString.fromHtml(
                        stringResource(
                            R.string.friends_add_by_tag_sheet_description,
                            "<font color=\"${Color(HL_USERNAME).asHexString(false)}\">",
                            "<font color=\"${Color(HL_TAG).asHexString(false)}\">",
                            "</font>",
                        )
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextField(
                        value = username,
                        onValueChange = {
                            // Cf. https://github.com/revoltchat/backend/blob/aab1734615ac3e09cd447d2c2862ae0f33f5ce5f/crates/delta/src/routes/onboard/complete.rs#L16
                            if (it.length <= 32 && it.all { char ->
                                    char.isLetterOrDigit() ||
                                            char == '_' || char == '.' || char == '-'
                                }) {
                                username = it
                            }
                        },
                        label = {
                            Text(stringResource(R.string.friends_add_by_tag_sheet_username))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        supportingText = {
                            Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        isError = error != null,
                        modifier = Modifier.weight(1f),
                    )
                    Text("#", modifier = Modifier.padding(bottom = 36.dp))
                    TextField(
                        value = tag,
                        onValueChange = {
                            // up to 4 characters, only numbers
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                tag = it
                            }
                        },
                        label = {
                            Text(stringResource(R.string.friends_add_by_tag_sheet_tag))
                        },
                        supportingText = {
                            Text("") // Layout hack
                        },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val clipboardText =
                                    clipboard.getClipEntry()?.clipData?.getItemAt(0)?.coerceToText(
                                        context
                                    ) ?: ""
                                try {
                                    if (clipboardText.isEmpty()) {
                                        showInvalidClipboardToast(context)
                                        return@launch
                                    }

                                    val parts = clipboardText.toString().replace(" ", "").split("#")
                                    if (parts.size == 2) {
                                        if (parts[0].length > 32 || parts[0].any { char ->
                                                !char.isLetterOrDigit() && char != '_' && char != '.' && char != '-'
                                            }) {
                                            showInvalidClipboardToast(context)
                                            return@launch
                                        }
                                        if (parts[1].length > 4 || parts[1].any { char -> !char.isDigit() }) {
                                            showInvalidClipboardToast(context)
                                            return@launch
                                        }

                                        username = parts[0].trim()
                                        tag = parts[1].trim()
                                    } else {
                                        showInvalidClipboardToast(context)
                                    }
                                } catch (_: Exception) {
                                    showInvalidClipboardToast(context)
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.friends_add_by_tag_sheet_paste_from_clipboard))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                if (username.isNotBlank() && tag.isNotBlank()) {
                                    try {
                                        error = null
                                        friendUser("$username#$tag")
                                        addByTagSheetVisible = false
                                    } catch (e: Exception) {
                                        error = e.localizedMessage ?: e.toString()
                                    }
                                }
                            }
                        },
                        enabled = username.isNotBlank() && tag.isNotBlank()
                    ) {
                        Text(stringResource(R.string.friends_add_by_tag_sheet_add))
                    }
                }
            }
        }
    }

    if (qrResult != null) {
        val qrResultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var contents by rememberSaveable { mutableStateOf<UserQRContents?>(null) }

        LaunchedEffect(qrResult) {
            when (qrResult) {
                is QRResult.QRUserCanceled -> {
                    // Silently dismiss ourselves.
                    qrResult = null
                }

                is QRResult.QRSuccess -> {
                    contents =
                        UserQR.fromUri((qrResult as QRResult.QRSuccess).content.rawValue ?: "")
                }

                else -> {
                    contents = null
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                qrResult = null
                contents = null
            },
            sheetGesturesEnabled = false,
            dragHandle = {},
            sheetState = qrResultSheetState,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    qrResult is QRResult.QRMissingPermission -> {
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_missing_permission_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_missing_permission_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                qrResult = null
                                contents = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }

                    qrResult is QRResult.QRSuccess && contents != null -> {
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_success_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        UserAvatar(
                            username = contents?.username ?: stringResource(R.string.unknown),
                            userId = contents?.id ?: "",
                            allowAnimation = false,
                            shape = RoundedCornerShape(LoadedSettings.avatarRadius),
                            size = 128.dp,
                            avatar = contents?.avatar?.let {
                                AutumnResource(
                                    tag = "avatars",
                                    id = it,
                                    metadata = Metadata(
                                        type = "image/png",
                                        width = 128,
                                        height = 128,
                                    )
                                )
                            }
                        )

                        Text(
                            text = buildAnnotatedString {
                                val displayNameSameAsUsername = contents?.displayName?.trim() ==
                                        contents?.username?.trim()

                                if (!displayNameSameAsUsername) {
                                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                    append(contents?.displayName)
                                    pop()
                                    append("\n")
                                }
                                append("${contents?.username}")
                                pushStyle(SpanStyle(fontWeight = FontWeight.ExtraLight))
                                append("#${contents?.discriminator}")
                                pop()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 21.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    contents?.let { userContents ->
                                        try {
                                            friendUser("${userContents.username}#${userContents.discriminator}")
                                            qrResult = null
                                            contents = null
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                e.localizedMessage ?: e.toString(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.friends_add_by_tag_sheet_add))
                        }
                    }

                    qrResult is QRResult.QRError || contents == null -> {
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_invalid_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_invalid_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                qrResult = null
                                contents = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }

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

            FloatingActionButtonMenu(
                modifier = Modifier.align(Alignment.BottomEnd),
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        modifier =
                            Modifier
                                .semantics {
                                    traversalIndex = -1f
                                }
                                .animateFloatingActionButton(
                                    visible = fabVisible || fabMenuExpanded,
                                    alignment = Alignment.BottomEnd
                                ),
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
                    ) {
                        val imageVector by remember {
                            derivedStateOf {
                                if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                            }
                        }
                        Icon(
                            painter = rememberVectorPainter(imageVector),
                            contentDescription = null,
                            modifier = Modifier.animateIcon({ checkedProgress })
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics { isTraversalGroup = true },
                    onClick = {
                        topNav.navigate("create/group")
                        fabMenuExpanded = false
                    },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_account_multiple_plus_24dp),
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(R.string.friends_new_group)) },
                )
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics { isTraversalGroup = true },
                    onClick = {
                        fabMenuExpanded = false
                        scanQrCodeLauncher.launch(null)
                    },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_qrcode_scan_24dp),
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(R.string.friends_scan_qr)) },
                )
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics { isTraversalGroup = true },
                    onClick = {
                        fabMenuExpanded = false
                        addByTagSheetVisible = true
                    },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_pound_24dp),
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(R.string.friends_add_by_tag)) },
                )
            }
        }
    }
}