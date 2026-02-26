package com.arslan.primenotify.ui.home

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.compose.material3.Switch
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.arslan.primenotify.R
import com.arslan.primenotify.service.isPrimeNotifyServiceEnabled
import com.arslan.primenotify.service.setPrimeNotifyServiceEnabled
import com.arslan.primenotify.ui.theme.PrimeNotifyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToFlashSettings: () -> Unit = {},
    onNavigateToWakeUpScreenSettings: () -> Unit = {},
    onNavigateToAODSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshState by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionItems = remember(refreshState) { buildPermissionItems(context) }
    val allPermissionsGranted = permissionItems.all { it.granted }

    var flashEnabled by rememberSaveable { mutableStateOf(false) }
    var wakeUpScreenEnabled by rememberSaveable { mutableStateOf(false) }
    var aodEnabled by rememberSaveable { mutableStateOf(false) }
    
    var isServiceActive by remember { mutableStateOf(isPrimeNotifyServiceEnabled(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val notificationScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.service_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isServiceActive) stringResource(R.string.service_active) else stringResource(R.string.service_disabled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isServiceActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isServiceActive,
                        onCheckedChange = { 
                            if (it && !allPermissionsGranted) {
                                showPermissionDialog = true
                            } else {
                                isServiceActive = it
                                setPrimeNotifyServiceEnabled(context, it)
                            }
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuleRow(
                        title = stringResource(R.string.flash_pattern_title),
                        description = stringResource(R.string.flash_pattern_desc),
                        checked = flashEnabled,
                        onCheckedChange = { flashEnabled = it },
                        onSettingsClick = onNavigateToFlashSettings
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    RuleRow(
                        title = stringResource(R.string.wake_up_screen_title),
                        description = stringResource(R.string.wake_up_screen_desc),
                        checked = wakeUpScreenEnabled,
                        onCheckedChange = { wakeUpScreenEnabled = it },
                        onSettingsClick = onNavigateToWakeUpScreenSettings
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    RuleRow(
                        title = stringResource(R.string.turn_on_aod_title),
                        description = stringResource(R.string.turn_on_aod_desc),
                        checked = aodEnabled,
                        onCheckedChange = { aodEnabled = it },
                        onSettingsClick = onNavigateToAODSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (remainingSeconds == 0) {
                        notificationScope.launch {
                            remainingSeconds = 4
                            while (remainingSeconds > 0) {
                                kotlinx.coroutines.delay(1000)
                                remainingSeconds--
                            }
                            sendTestNotification(context)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = remainingSeconds == 0
            ) {
                Text(stringResource(R.string.send_test_notification))
            }

            if (remainingSeconds > 0) {
                Text(
                    text = stringResource(R.string.sending_in_seconds, remainingSeconds),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { 
                Text(
                    stringResource(R.string.permission_required_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Text(stringResource(R.string.permission_required_desc))
            },
            confirmButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.got_it))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun RuleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings_for, title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange
            ) {
                Icon(
                    imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (checked) stringResource(R.string.cd_uncheck_item, title) else stringResource(R.string.cd_check_item, title),
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PrimeNotifyTheme {
        HomeScreen()
    }
}

enum class PermissionType {
    NotificationAccess,
    PostNotifications,
    Camera,
    NotificationPolicy,
    IgnoreBatteryOptimizations,
    WriteSecureSettings
}

data class PermissionItem(
    val type: PermissionType,
    val title: String,
    val description: String,
    val granted: Boolean
)

fun buildPermissionItems(context: Context): List<PermissionItem> {
    val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
    val postNotificationsGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val notificationAccessGranted = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)

    val notificationPolicyGranted = notificationManager?.isNotificationPolicyAccessGranted == true

    val powerManager = context.getSystemService(PowerManager::class.java)
    val ignoreBatteryOptimizationsGranted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

    val writeSecureSettingsGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.WRITE_SECURE_SETTINGS
    ) == PackageManager.PERMISSION_GRANTED

    return listOf(
        PermissionItem(
            type = PermissionType.NotificationAccess,
            title = context.getString(R.string.permission_notification_access),
            description = context.getString(R.string.permission_notification_access_desc),
            granted = notificationAccessGranted
        ),
        PermissionItem(
            type = PermissionType.PostNotifications,
            title = context.getString(R.string.permission_post_notifications),
            description = context.getString(R.string.permission_post_notifications_desc),
            granted = postNotificationsGranted
        ),
        PermissionItem(
            type = PermissionType.Camera,
            title = context.getString(R.string.permission_camera_flash),
            description = context.getString(R.string.permission_camera_flash_desc),
            granted = cameraGranted
        ),
        PermissionItem(
            type = PermissionType.NotificationPolicy,
            title = context.getString(R.string.permission_notification_policy),
            description = context.getString(R.string.permission_notification_policy_desc),
            granted = notificationPolicyGranted
        ),
        PermissionItem(
            type = PermissionType.IgnoreBatteryOptimizations,
            title = context.getString(R.string.permission_run_background),
            description = context.getString(R.string.permission_run_background_desc),
            granted = ignoreBatteryOptimizationsGranted
        ),
        PermissionItem(
            type = PermissionType.WriteSecureSettings,
            title = context.getString(R.string.permission_secure_settings),
            description = context.getString(R.string.permission_secure_settings_desc),
            granted = writeSecureSettingsGranted
        )
    )
}

private fun sendTestNotification(context: Context) {
    val channelId = "primenotify_test_channel_popup"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.getString(R.string.test_notification_channel)
        val descriptionText = context.getString(R.string.test_notification_channel_desc)
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(context.getString(R.string.test_notification_title))
        .setContentText(context.getString(R.string.test_notification_text))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        try {
            notify(1001, builder.build())
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }
}
