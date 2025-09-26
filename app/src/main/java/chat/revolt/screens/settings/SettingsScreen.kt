package chat.revolt.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.activities.InviteActivity
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.UpdateChecker
import chat.revolt.api.internals.UpdateInfo
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.composables.generic.ListHeader
import chat.revolt.composables.generic.UpdateBanner
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import android.content.Context

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val updateChecker = UpdateChecker(context, kvStorage)
    
    var isUpdateCheckerEnabled by mutableStateOf(false)
        private set
    
    var isCheckingForUpdates by mutableStateOf(false)
        private set
    
    var manualCheckResult by mutableStateOf<String?>(null)
        private set
    
    var foundUpdate by mutableStateOf<UpdateInfo?>(null)
        private set
    
    init {
        viewModelScope.launch {
            isUpdateCheckerEnabled = updateChecker.isUpdateCheckerEnabled()
        }
    }
    
    fun toggleUpdateChecker(enabled: Boolean) {
        isUpdateCheckerEnabled = enabled
        viewModelScope.launch {
            updateChecker.setUpdateCheckerEnabled(enabled)
        }
    }
    
    fun manualCheckForUpdates() {
        if (isCheckingForUpdates) return
        
        viewModelScope.launch {
            isCheckingForUpdates = true
            manualCheckResult = null
            foundUpdate = null
            
            try {
                val updateInfo = updateChecker.checkForUpdates()
                if (updateInfo != null) {
                    foundUpdate = updateInfo
                    manualCheckResult = context.getString(R.string.update_check_available, updateInfo.version)
                } else {
                    manualCheckResult = context.getString(R.string.update_check_up_to_date)
                }
            } catch (e: Exception) {
                manualCheckResult = context.getString(R.string.update_check_failed)
            } finally {
                isCheckingForUpdates = false
            }
        }
    }
    
    fun clearManualCheckResult() {
        manualCheckResult = null
        foundUpdate = null
    }
    
    fun logout() {
        runBlocking {
            kvStorage.remove("sessionToken")
            LoadedSettings.reset()
            RevoltAPI.logout()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.icn_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 10.dp)
                ) {
                    viewModel.foundUpdate?.let { updateInfo ->
                        UpdateBanner(
                            updateInfo = updateInfo,
                            onDismiss = { viewModel.clearManualCheckResult() }
                        )
                    }
                    
                    ListHeader {
                        Text(stringResource(R.string.settings_category_account))
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_profile)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_id_card_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_profile")
                            .clickable {
                                navController.navigate("settings/profile")
                            }
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_sessions)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_devices_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_sessions")
                            .clickable {
                                navController.navigate("settings/sessions")
                            }
                    )

                    ListHeader {
                        Text(stringResource(R.string.settings_category_general))
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_appearance)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_palette_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_appearance")
                            .clickable {
                                navController.navigate("settings/appearance")
                            }
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_language)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_language_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_language")
                            .clickable {
                                navController.navigate("settings/language")
                            }
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_chat)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_chat_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_chat")
                            .clickable {
                                navController.navigate("settings/chat")
                            }
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_notifications)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_notification_settings_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_notifications")
                            .clickable {
                                navController.navigate("settings/notifications")
                            }
                    )

                    ListItem(
                        headlineContent = {
                            Text(text = "App Updates")
                        },
                        supportingContent = {
                            Column {
                                viewModel.manualCheckResult?.let { result ->
                                    Text(
                                        text = result,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (viewModel.foundUpdate != null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_download_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Manual check button - more compact
                                Button(
                                    onClick = { viewModel.manualCheckForUpdates() },
                                    enabled = !viewModel.isCheckingForUpdates
                                ) {
                                    if (viewModel.isCheckingForUpdates) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.width(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(stringResource(R.string.update_check_now))
                                    }
                                }
                                
                                Switch(
                                    checked = viewModel.isUpdateCheckerEnabled,
                                    onCheckedChange = viewModel::toggleUpdateChecker
                                )
                            }
                        }
                    )

                    ListHeader {
                        Text(stringResource(R.string.settings_category_miscellaneous))
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.about)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_info_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_about")
                            .clickable {
                                navController.navigate("about")
                            }
                    )

                    if (BuildConfig.DEBUG) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Debug"
                                )
                            },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_sign_language_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("settings_view_debug")
                                .clickable {
                                    navController.navigate("settings/debug")
                                }
                        )
                    }

                    if (FeatureFlags.labsAccessControlGranted) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Labs"
                                )
                            },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_sign_language_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("settings_view_labs")
                                .clickable {
                                    navController.navigate("labs")
                                }
                        )
                    }

                    if (LoadedSettings.experimentsEnabled) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Experiments"
                                )
                            },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_brand_family_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("settings_view_experiments")
                                .clickable {
                                    navController.navigate("settings/experiments")
                                }
                        )
                    }

                    ListHeader {
                        Text(
                            stringResource(
                                R.string.settings_category_last,
                                BuildConfig.VERSION_NAME
                            )
                        )
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_changelogs)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_wand_shine_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_changelogs")
                            .clickable {
                                navController.navigate("settings/changelogs")
                            }
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_feedback)
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.settings_feedback_description)
                            )
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.icn_feedback_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_feedback")
                            .clickable {
                                val intent = Intent(
                                    context,
                                    InviteActivity::class.java
                                ).setAction(Intent.ACTION_VIEW)

                                intent.data = "https://rvlt.gg/tyKYccTr".toUri()
                                context.startActivity(intent)
                            }
                    )

                    ListItem(
                        headlineContent = {
                            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                                Text(
                                    text = stringResource(id = R.string.logout)
                                )
                            }
                        },
                        leadingContent = {
                            SettingsIcon(danger = true) {
                                Icon(
                                    painter = painterResource(R.drawable.icn_logout_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_view_logout")
                            .clickable {
                                viewModel.logout()
                                navController.navigate("login/greeting") {
                                    popUpTo("chat") {
                                        inclusive = true
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsIcon(danger: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides
                if (danger) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onBackground
    ) {
        content()
    }
}
