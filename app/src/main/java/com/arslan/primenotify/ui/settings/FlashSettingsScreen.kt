package com.arslan.primenotify.ui.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.data.FlashPattern
import com.arslan.primenotify.data.FlashRule
import com.arslan.primenotify.data.RulesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditRule: (String?) -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
    var rules by remember { mutableStateOf(rulesManager.getFlashRules()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flash Pattern Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                onNavigateToAddEditRule(null)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No rules configured.\nTap + to add a new rule.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = { isEnabled ->
                                rulesManager.toggleFlashRule(rule.id, isEnabled)
                                rules = rulesManager.getFlashRules()
                            },
                            onEdit = {
                                onNavigateToAddEditRule(rule.id)
                            },
                            onDelete = {
                                rulesManager.removeFlashRule(rule.id)
                                rules = rulesManager.getFlashRules()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleCard(
    rule: FlashRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (rule.appNames.isNotEmpty()) rule.appNames.joinToString(", ") else "No Apps Chosen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (rule.keywords.isEmpty()) "Keywords: Any" else "Keywords: ${rule.keywords.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pattern: ${rule.pattern.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Rule",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Rule",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}



@Composable
fun AppRow(
    app: AppItem,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actionIconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val iconBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = IconCache.get(app.packageName),
        key1 = app.packageName
    ) {
        if (value == null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val drawable = pm.getApplicationIcon(app.packageName)
                    val bitmap = drawableToImageBitmap(drawable)
                    IconCache.put(app.packageName, bitmap)
                    bitmap
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val currentIcon = iconBitmap
        if (currentIcon != null) {
            Image(
                bitmap = currentIcon,
                contentDescription = app.name,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionIcon != null) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = actionIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun drawableToImageBitmap(drawable: Drawable): androidx.compose.ui.graphics.ImageBitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap.asImageBitmap()
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

data class AppItem(val name: String, val packageName: String)

fun getInstalledApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return packages
        .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
        .map { 
            AppItem(
                name = pm.getApplicationLabel(it).toString(),
                packageName = it.packageName
            )
        }
        .sortedBy { it.name.lowercase() }
}

object IconCache {
    private val cache = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(150)

    fun get(packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
        return cache.get(packageName)
    }

    fun put(packageName: String, bitmap: androidx.compose.ui.graphics.ImageBitmap) {
        cache.put(packageName, bitmap)
    }
}
