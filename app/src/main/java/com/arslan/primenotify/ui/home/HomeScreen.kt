package com.arslan.primenotify.ui.home

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.primenotify.service.isPrimeNotifyServiceEnabled
import com.arslan.primenotify.service.setPrimeNotifyServiceEnabled
import com.arslan.primenotify.ui.theme.PrimeNotifyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
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
    val allPermissionsGranted = permissionItems.all { it.granted }
    val grantedPermissionCount = permissionItems.count { it.granted }
    val totalPermissionCount = permissionItems.size
    var permissionExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            permissionExpanded = false
        }
    }

    var regexFlashEnabled by rememberSaveable { mutableStateOf(false) }
    var wakeUpScreenEnabled by rememberSaveable { mutableStateOf(false) }
    var aodEnabled by rememberSaveable { mutableStateOf(false) }
    var regexPattern by rememberSaveable { mutableStateOf("^OTP:.*$") }

    val primeNotifyServiceEnabled = isPrimeNotifyServiceEnabled(context)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PrimeNotify",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Switch(
                        checked = primeNotifyServiceEnabled,
                        onCheckedChange = {
                            if (!allPermissionsGranted && it) return@Switch
                            setPrimeNotifyServiceEnabled(context, it)
                            refreshState++
                        },
                        enabled = allPermissionsGranted || primeNotifyServiceEnabled
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
            
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { 

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        permissionExpanded = !permissionExpanded
                    },
                border = BorderStroke(
                    width = 2.dp,
                    color = if (allPermissionsGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (allPermissionsGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$grantedPermissionCount/$totalPermissionCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (allPermissionsGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (permissionExpanded) {
                permissionItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        colors = CardDefaults.cardColors(
                             MaterialTheme.colorScheme.surfaceContainer
                            
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (item.granted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                    color = if (item.granted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

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

                                        PermissionType.Vibrate,
                                        PermissionType.ModifyAudio,
                                        PermissionType.WakeLock -> Unit
                                    }
                                },
                                enabled = !item.granted
                            ) {
                                Text(if (item.granted) "Verildi" else "Ver")
                            }
                        }
                    }
                }
            }

            Text(
                text = "Ana Kontroller",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleRow(
                        title = "Regex Flash Pattern",
                        description = "Bildirim metnine göre özel flaş deseni",
                        checked = regexFlashEnabled,
                        onCheckedChange = { regexFlashEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    ToggleRow(
                        title = "Wake Up Screen",
                        description = "Eşleşen bildirimi alınca ekranı uyandır",
                        checked = wakeUpScreenEnabled,
                        onCheckedChange = { wakeUpScreenEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    ToggleRow(
                        title = "Turn On AOD",
                        description = "Ekran kapalıysa AOD görünümünü aç",
                        checked = aodEnabled,
                        onCheckedChange = { aodEnabled = it }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Regex Kuralı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("MVP") }
                        )
                    }

                    OutlinedTextField(
                        value = regexPattern,
                        onValueChange = { regexPattern = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Pattern") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {}) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PrimeNotifyTheme {
        HomeScreen()
    }
}

private enum class PermissionType {
    NotificationAccess,
    PostNotifications,
    Camera,
    WriteSettings,
    NotificationPolicy,
    Vibrate,
    ModifyAudio,
    WakeLock
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

    return listOf(
        PermissionItem(
            type = PermissionType.NotificationAccess,
            title = "Notification Access",
            description = "Regex eşleşmesini algılamak için bildirim erişimi",
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
            type = PermissionType.Vibrate,
            title = "Vibrate",
            description = "Titreşim motorunu kontrol etmek için",
            granted = true
        ),
        PermissionItem(
            type = PermissionType.ModifyAudio,
            title = "Modify Audio Settings",
            description = "Mevcut zil/titreşim ayarlarını düzenlemek için",
            granted = true
        ),
        PermissionItem(
            type = PermissionType.WakeLock,
            title = "Wake Lock",
            description = "Ekranı uyandırma senaryoları için",
            granted = true
        )
    )
}
