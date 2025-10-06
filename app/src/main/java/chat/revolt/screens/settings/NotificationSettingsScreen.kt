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

    var isBatteryOptimizationDisabled by mutableStateOf(false)
        private set

    var notificationMode by mutableStateOf(chat.revolt.services.NotificationMode.OFF)
        private set

    var pollingInterval by mutableStateOf(15L)
        private set

    var canScheduleExactAlarms by mutableStateOf(true)
        private set

    init {
        loadSettings()
        checkBatteryOptimization()
        checkExactAlarmPermission()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            notificationMode = chat.revolt.api.settings.NotificationSettingsProvider.getNotificationMode()
            pollingInterval = chat.revolt.api.settings.NotificationSettingsProvider.getPollingInterval()

            if (hasNotificationPermission()) {
                when (notificationMode) {
                    chat.revolt.services.NotificationMode.INSTANT -> {
                        chat.revolt.services.NotificationPollingService.stop(context)
                        chat.revolt.services.NotificationForegroundService.start(context)
                    }
                    chat.revolt.services.NotificationMode.BATTERY_SAVER -> {
                        chat.revolt.services.NotificationForegroundService.stop(context)
                        chat.revolt.services.NotificationPollingService.start(context, pollingInterval)
                    }
                    chat.revolt.services.NotificationMode.OFF -> {
                        chat.revolt.services.NotificationForegroundService.stop(context)
                        chat.revolt.services.NotificationPollingService.stop(context)
                    }
                }
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


    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            isBatteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            isBatteryOptimizationDisabled = true
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            canScheduleExactAlarms = alarmManager.canScheduleExactAlarms()
        } else {
            canScheduleExactAlarms = true
        }
    }

    fun updateNotificationMode(mode: chat.revolt.services.NotificationMode, onPermissionRequired: () -> Unit) {
        if (mode != chat.revolt.services.NotificationMode.OFF && !hasNotificationPermission()) {
            onPermissionRequired()
            return
        }

        notificationMode = mode

        viewModelScope.launch {
            chat.revolt.api.settings.NotificationSettingsProvider.setNotificationMode(mode)

            when (mode) {
                chat.revolt.services.NotificationMode.INSTANT -> {
                    chat.revolt.services.NotificationPollingService.stop(context)
                    chat.revolt.services.NotificationForegroundService.start(context)
                }
                chat.revolt.services.NotificationMode.BATTERY_SAVER -> {
                    chat.revolt.services.NotificationForegroundService.stop(context)
                    chat.revolt.services.NotificationPollingService.start(context, pollingInterval)
                }
                chat.revolt.services.NotificationMode.OFF -> {
                    chat.revolt.services.NotificationForegroundService.stop(context)
                    chat.revolt.services.NotificationPollingService.stop(context)
                }
            }
        }
    }

    fun updatePollingInterval(intervalMinutes: Long) {
        pollingInterval = intervalMinutes

        viewModelScope.launch {
            chat.revolt.api.settings.NotificationSettingsProvider.setPollingInterval(intervalMinutes)

            if (notificationMode == chat.revolt.services.NotificationMode.BATTERY_SAVER) {
                chat.revolt.services.NotificationPollingService.stop(context)
                chat.revolt.services.NotificationPollingService.start(context, intervalMinutes)
            }
        }
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open exact alarm settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshExactAlarmPermission() {
        checkExactAlarmPermission()
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
                    Text(stringResource(R.string.settings_notifications_mode_instant))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_notifications_mode_instant_description))
                },
                leadingContent = {
                    androidx.compose.material3.RadioButton(
                        selected = viewModel.notificationMode == chat.revolt.services.NotificationMode.INSTANT,
                        onClick = null
                    )
                },
                modifier = Modifier.clickable {
                    viewModel.updateNotificationMode(chat.revolt.services.NotificationMode.INSTANT, onPermissionRequired)
                }
            )

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_notifications_mode_battery_saver))
                },
                supportingContent = {
                    val intervalText = when (viewModel.pollingInterval) {
                        5L -> stringResource(R.string.settings_notifications_interval_5min)
                        10L -> stringResource(R.string.settings_notifications_interval_10min)
                        15L -> stringResource(R.string.settings_notifications_interval_15min)
                        30L -> stringResource(R.string.settings_notifications_interval_30min)
                        60L -> stringResource(R.string.settings_notifications_interval_60min)
                        else -> "${viewModel.pollingInterval} minutes"
                    }
                    Text(stringResource(R.string.settings_notifications_mode_battery_saver_description, intervalText))
                },
                leadingContent = {
                    androidx.compose.material3.RadioButton(
                        selected = viewModel.notificationMode == chat.revolt.services.NotificationMode.BATTERY_SAVER,
                        onClick = null
                    )
                },
                modifier = Modifier.clickable {
                    viewModel.updateNotificationMode(chat.revolt.services.NotificationMode.BATTERY_SAVER, onPermissionRequired)
                }
            )

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_notifications_mode_off))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_notifications_mode_off_description))
                },
                leadingContent = {
                    androidx.compose.material3.RadioButton(
                        selected = viewModel.notificationMode == chat.revolt.services.NotificationMode.OFF,
                        onClick = null
                    )
                },
                modifier = Modifier.clickable {
                    viewModel.updateNotificationMode(chat.revolt.services.NotificationMode.OFF, onPermissionRequired)
                }
            )

            if (viewModel.notificationMode == chat.revolt.services.NotificationMode.BATTERY_SAVER) {
                Spacer(modifier = Modifier.height(8.dp))

                if (!viewModel.canScheduleExactAlarms && (viewModel.pollingInterval == 5L || viewModel.pollingInterval == 10L)) {
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_notifications_exact_alarm_required),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.settings_notifications_exact_alarm_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = { viewModel.openExactAlarmSettings() }
                            ) {
                                Text(stringResource(R.string.settings_notifications_grant_exact_alarm))
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.settings_notifications_interval_header),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                val intervals = listOf(5L to R.string.settings_notifications_interval_5min, 10L to R.string.settings_notifications_interval_10min, 15L to R.string.settings_notifications_interval_15min, 30L to R.string.settings_notifications_interval_30min, 60L to R.string.settings_notifications_interval_60min)

                intervals.forEach { (minutes, labelRes) ->
                    val batteryImpact = when (minutes) {
                        5L -> stringResource(R.string.settings_notifications_battery_very_high)
                        10L -> stringResource(R.string.settings_notifications_battery_high)
                        15L -> stringResource(R.string.settings_notifications_battery_moderate)
                        30L -> stringResource(R.string.settings_notifications_battery_low)
                        else -> stringResource(R.string.settings_notifications_battery_very_low)
                    }

                    val needsPermission = (minutes == 5L || minutes == 10L) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    val enabled = if (needsPermission) viewModel.canScheduleExactAlarms else true

                    ListItem(
                        headlineContent = {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(labelRes))
                                Text(
                                    text = batteryImpact,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (minutes <= 10) {
                                        MaterialTheme.colorScheme.error
                                    } else if (minutes == 15L) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                )
                            }
                        },
                        leadingContent = {
                            androidx.compose.material3.RadioButton(
                                selected = viewModel.pollingInterval == minutes,
                                onClick = null,
                                enabled = enabled
                            )
                        },
                        modifier = Modifier.clickable(enabled = enabled) {
                            viewModel.updatePollingInterval(minutes)
                        }
                    )
                }

                if (viewModel.pollingInterval <= 10) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_notifications_doze_warning_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = stringResource(R.string.settings_notifications_doze_warning_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

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
                    viewModel.refreshExactAlarmPermission()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(Unit) {
            viewModel.refreshBatteryOptimizationStatus()
            viewModel.refreshExactAlarmPermission()
        }
    }
}