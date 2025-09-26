package chat.revolt.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.composables.generic.ListHeader
import chat.revolt.persistence.KVStorage
import chat.revolt.services.NotificationForegroundService
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kvStorage: KVStorage
) : ViewModel() {
    companion object {
        private const val KEY_BACKGROUND_SERVICE_ENABLED = "notification_background_service_enabled"
    }

    var isBackgroundServiceEnabled by mutableStateOf(false)
        private set

    var isBatteryOptimizationDisabled by mutableStateOf(false)
        private set

    init {
        loadSettings()
        checkBatteryOptimization()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            isBackgroundServiceEnabled = kvStorage.getBoolean(KEY_BACKGROUND_SERVICE_ENABLED) ?: false

            if (isBackgroundServiceEnabled && hasNotificationPermission()) {
                NotificationForegroundService.start(context)
            }
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission wasn't required in older versions. (wonder if this is worth having with the api requirement?)
        }
    }

    fun toggleBackgroundService(enabled: Boolean, onPermissionRequired: () -> Unit) {
        if (enabled && !hasNotificationPermission()) {
            onPermissionRequired()
            return
        }
        isBackgroundServiceEnabled = enabled

        viewModelScope.launch {
            kvStorage.set(KEY_BACKGROUND_SERVICE_ENABLED, enabled)
        }

        if (enabled) {
            NotificationForegroundService.start(context)
        } else {
            NotificationForegroundService.stop(context)
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            isBatteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            isBatteryOptimizationDisabled = true // Not applicable for older versions
        }
    }

    fun openNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Toast.makeText(context, context.getString(R.string.toast_opening_battery_settings), Toast.LENGTH_SHORT).show()
                } else {
                    throw SecurityException("Intent not resolvable")
                }
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent().apply {
                        action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                    if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(fallbackIntent)
                        Toast.makeText(context, context.getString(R.string.toast_find_battery_optimization), Toast.LENGTH_LONG).show()
                    } else {
                        throw SecurityException("Fallback intent not resolvable")
                    }
                } catch (e2: Exception) {
                    try {
                        val appSettingsIntent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }

                        if (appSettingsIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(appSettingsIntent)
                            Toast.makeText(context, context.getString(R.string.toast_find_battery_in_app_info), Toast.LENGTH_LONG).show()
                        } else {
                            throw SecurityException("App settings intent not resolvable")
                        }
                    } catch (e3: Exception) {
                        Toast.makeText(context, context.getString(R.string.toast_battery_settings_manual), Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.toast_battery_optimization_not_available), Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshBatteryOptimizationStatus() {
        checkBatteryOptimization()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationSettingsScreenViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionState?.status?.isGranted == true
    } else {
        true // Permission wasn't required in older versions
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && !viewModel.isBackgroundServiceEnabled) {
            viewModel.toggleBackgroundService(true) {}
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_notifications),
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
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(pv)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (!hasPermission) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_notifications_permission_required),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_notifications_permission_denied),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    notificationPermissionState?.launchPermissionRequest()
                                        ?: viewModel.openNotificationSettings()
                                }
                            ) {
                                Text(stringResource(R.string.settings_notifications_grant_permission))
                            }
                            OutlinedButton(
                                onClick = viewModel::openNotificationSettings
                            ) {
                                Text(stringResource(R.string.settings_notifications_open_system_settings))
                            }
                        }
                    }
                }
            }

            ListHeader {
                Text(
                    text = stringResource(R.string.settings_notifications_header),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = stringResource(R.string.settings_notifications_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val onPermissionRequired = {
                notificationPermissionState?.launchPermissionRequest()
                    ?: viewModel.openNotificationSettings()
            }

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_notifications_background_service))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_notifications_background_service_description))
                },
                trailingContent = {
                    Switch(
                        checked = viewModel.isBackgroundServiceEnabled,
                        onCheckedChange = null,
                        enabled = hasPermission
                    )
                },
                modifier = Modifier.clickable(enabled = hasPermission || !viewModel.isBackgroundServiceEnabled) {
                    viewModel.toggleBackgroundService(
                        !viewModel.isBackgroundServiceEnabled,
                        onPermissionRequired
                    )
                }
            )

            // Battery Optimization Settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_notifications_battery_optimization))
                    },
                    supportingContent = {
                        Column {
                            Text(stringResource(R.string.settings_notifications_battery_optimization_description))
                            Text(
                                text = if (viewModel.isBatteryOptimizationDisabled) {
                                    stringResource(R.string.settings_notifications_battery_not_optimized)
                                } else {
                                    stringResource(R.string.settings_notifications_battery_optimized)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (viewModel.isBatteryOptimizationDisabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    trailingContent = {
                        OutlinedButton(
                            onClick = {
                                viewModel.openBatteryOptimizationSettings()
                            }
                        ) {
                            Text(stringResource(R.string.settings_notifications_open_battery_settings))
                        }
                    }
                )
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshBatteryOptimizationStatus()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(Unit) {
            viewModel.refreshBatteryOptimizationStatus()
        }
    }
}