package com.arslan.primenotify.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.primenotify.R
import com.arslan.primenotify.ui.theme.PrimeNotifyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(modifier: Modifier = Modifier) {
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

    val requestPostNotificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshState++
    }

    val requestCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshState++
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshState++
    }

    var showAdbDialog by remember { mutableStateOf(false) }

    val permissionItems = remember(refreshState) { buildPermissionItems(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            permissionItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.granted) {
                        OutlinedButton(
                            onClick = {},
                            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                            enabled = false
                        ) {
                            Text(stringResource(R.string.permission_granted))
                        }
                    } else {
                        Button(
                            onClick = {
                                when (item.type) {
                                    PermissionType.PostNotifications -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestPostNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }

                                    PermissionType.Camera -> {
                                        requestCameraLauncher.launch(Manifest.permission.CAMERA)
                                    }

                                    PermissionType.NotificationAccess -> {
                                        settingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    }

                                    PermissionType.IgnoreBatteryOptimizations -> {
                                        settingsLauncher.launch(
                                            Intent(
                                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }

                                    PermissionType.WriteSecureSettings -> {
                                        showAdbDialog = true
                                    }

                                    PermissionType.OverlayPermission -> {
                                        settingsLauncher.launch(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.permission_grant))
                        }
                    }
                }
            }
        }
    
    if (showAdbDialog) {
        val adbCommand = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
        AlertDialog(
            onDismissRequest = { showAdbDialog = false },
            title = { 
                Text(
                    stringResource(R.string.grant_secure_settings_title), 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.grant_secure_settings_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.grant_secure_settings_adb_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = adbCommand,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ADB Command", adbCommand)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, context.getString(R.string.command_copied), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(stringResource(R.string.copy))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAdbDialog = false
                    refreshState++
                }) {
                    Text(stringResource(R.string.got_it))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdbDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionsScreenPreview() {
    PrimeNotifyTheme {
        PermissionsScreen()
    }
}

private enum class PermissionType {
    NotificationAccess,
    PostNotifications,
    Camera,
    IgnoreBatteryOptimizations,
    WriteSecureSettings,
    OverlayPermission
}

private data class PermissionItem(
    val type: PermissionType,
    val title: String,
    val description: String,
    val granted: Boolean
)

private fun buildPermissionItems(context: Context): List<PermissionItem> {
    val postNotificationsGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val notificationAccessGranted = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)

    val powerManager = context.getSystemService(PowerManager::class.java)
    val ignoreBatteryOptimizationsGranted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

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
            type = PermissionType.IgnoreBatteryOptimizations,
            title = context.getString(R.string.permission_run_background),
            description = context.getString(R.string.permission_run_background_desc),
            granted = ignoreBatteryOptimizationsGranted
        ),
        PermissionItem(
            type = PermissionType.WriteSecureSettings,
            title = context.getString(R.string.permission_secure_settings),
            description = context.getString(R.string.permission_secure_settings_desc),
            granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        ),
        PermissionItem(
            type = PermissionType.OverlayPermission,
            title = context.getString(R.string.permission_overlay_title),
            description = context.getString(R.string.permission_overlay_desc),
            granted = Settings.canDrawOverlays(context)
        )
    )
}
