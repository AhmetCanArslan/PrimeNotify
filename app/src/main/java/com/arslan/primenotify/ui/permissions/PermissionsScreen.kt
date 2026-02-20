package com.arslan.primenotify.ui.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                            Text("Granted")
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

                                    PermissionType.WriteSettings -> {
                                        settingsLauncher.launch(
                                            Intent(
                                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }

                                    PermissionType.NotificationPolicy -> {
                                        settingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                    }

                                    PermissionType.IgnoreBatteryOptimizations -> {
                                        settingsLauncher.launch(
                                            Intent(
                                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }
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
    WriteSettings,
    NotificationPolicy,
    IgnoreBatteryOptimizations
}

private data class PermissionItem(
    val type: PermissionType,
    val title: String,
    val description: String,
    val granted: Boolean
)

private fun buildPermissionItems(context: Context): List<PermissionItem> {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
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

    val writeSettingsGranted = Settings.System.canWrite(context)

    val notificationPolicyGranted = notificationManager?.isNotificationPolicyAccessGranted == true

    val powerManager = context.getSystemService(PowerManager::class.java)
    val ignoreBatteryOptimizationsGranted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

    return listOf(
        PermissionItem(
            type = PermissionType.NotificationAccess,
            title = "Notification Access",
            description = "Belirli bildirimleri algılamak için bildirim erişimi",
            granted = notificationAccessGranted
        ),
        PermissionItem(
            type = PermissionType.PostNotifications,
            title = "Post Notifications",
            description = "PrimeNotify'nin kendi bildirimlerini gösterebilmesi için",
            granted = postNotificationsGranted
        ),
        PermissionItem(
            type = PermissionType.Camera,
            title = "Camera / Flash",
            description = "Flaş pattern çalıştırmak için kamera-flash erişimi",
            granted = cameraGranted
        ),
        PermissionItem(
            type = PermissionType.WriteSettings,
            title = "Modify System Settings",
            description = "AOD ve sistem titreşim/ayar kontrolü için",
            granted = writeSettingsGranted
        ),
        PermissionItem(
            type = PermissionType.NotificationPolicy,
            title = "Notification Policy Access",
            description = "Zil ve titreşim davranışını değiştirebilmek için",
            granted = notificationPolicyGranted
        ),
        PermissionItem(
            type = PermissionType.IgnoreBatteryOptimizations,
            title = "Run in Background",
            description = "Hizmetin pil tasarrufu tarafından kapatılmasını önler",
            granted = ignoreBatteryOptimizationsGranted
        )
    )
}
