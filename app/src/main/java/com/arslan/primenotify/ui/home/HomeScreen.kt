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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
    
    val coroutineScope = rememberCoroutineScope()
    var remainingSeconds by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuleRow(
                        title = "Flash Pattern",
                        description = "Bildirim metnine göre özel flaş deseni",
                        checked = flashEnabled,
                        onCheckedChange = { flashEnabled = it },
                        onSettingsClick = onNavigateToFlashSettings
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    RuleRow(
                        title = "Wake Up Screen",
                        description = "Eşleşen bildirimi alınca ekranı uyandır",
                        checked = wakeUpScreenEnabled,
                        onCheckedChange = { wakeUpScreenEnabled = it },
                        onSettingsClick = onNavigateToWakeUpScreenSettings
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    RuleRow(
                        title = "Turn On AOD",
                        description = "Ekran kapalıysa AOD görünümünü aç",
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
                        coroutineScope.launch {
                            remainingSeconds = 7
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
                Text("Send Test Notification")
            }

            if (remainingSeconds > 0) {
                Text(
                    text = "Sending in $remainingSeconds seconds...",
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
                    contentDescription = "Settings for $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange
            ) {
                Icon(
                    imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (checked) "Uncheck $title" else "Check $title",
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
    WriteSettings,
    NotificationPolicy,
    Vibrate,
    ModifyAudio,
    WakeLock
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

    val writeSettingsGranted = android.provider.Settings.System.canWrite(context)

    val notificationPolicyGranted = notificationManager?.isNotificationPolicyAccessGranted == true

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

private fun sendTestNotification(context: Context) {
    val channelId = "primenotify_test_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Test Notifications"
        val descriptionText = "Channel for test notifications"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("PrimeNotify Test")
        .setContentText("This is a test notification to verify rules.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    with(NotificationManagerCompat.from(context)) {
        try {
            notify(1001, builder.build())
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }
}
