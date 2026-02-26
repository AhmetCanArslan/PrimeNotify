package com.arslan.primenotify.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)

object AppListManager {
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        if (_installedApps.value.isNotEmpty()) return
        val appContext = context.applicationContext
        applicationScope.launch {
            val pm = appContext.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val apps = packages
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { appInfo ->
                    val icon = try {
                        drawableToImageBitmap(pm.getApplicationIcon(appInfo.packageName))
                    } catch (_: Exception) { null }
                    AppItem(
                        name = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName,
                        icon = icon
                    )
                }
                .sortedBy { it.name.lowercase() }
            _installedApps.value = apps
        }
    }

    fun getIconForPackage(packageName: String): ImageBitmap? =
        _installedApps.value.find { it.packageName == packageName }?.icon

    private const val ICON_SIZE_PX = 48

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        val rawBitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        val scaled = if (rawBitmap.width > ICON_SIZE_PX || rawBitmap.height > ICON_SIZE_PX) {
            Bitmap.createScaledBitmap(rawBitmap, ICON_SIZE_PX, ICON_SIZE_PX, true)
        } else {
            rawBitmap
        }
        return scaled.asImageBitmap()
    }
}

@Composable
fun AppRow(
    app: AppItem,
    actionIcon: ImageVector? = null,
    actionIconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(40.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

@Composable
fun AppSelectionTable(
    installedApps: List<AppItem>,
    selectedApps: List<AppItem>,
    onSelectedAppsChanged: (List<AppItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = installedApps.isEmpty()
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }

    // Pre-compute set for O(1) lookups instead of O(n) list contains
    val selectedPackages = remember(selectedApps) {
        selectedApps.map { it.packageName }.toHashSet()
    }
    val unselectedApps = remember(installedApps, selectedPackages) {
        installedApps.filter { it.packageName !in selectedPackages }
    }

    // Filter apps based on search query
    val filteredUnselectedApps = remember(unselectedApps, searchQuery) {
        if (searchQuery.isBlank()) unselectedApps
        else unselectedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredSelectedApps = remember(selectedApps, searchQuery) {
        if (searchQuery.isBlank()) selectedApps
        else selectedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.target_apps), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_apps)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Left Column — Available
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(stringResource(R.string.available), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredUnselectedApps, key = { it.packageName }, contentType = { "app" }) { app ->
                                AppRow(
                                    app = app,
                                    actionIcon = Icons.Default.Add,
                                    onClick = { onSelectedAppsChanged(selectedApps + app) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    VerticalDivider()
                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Column — Selected
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.selected), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            IconButton(
                                onClick = { onSelectedAppsChanged(unselectedApps) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.cd_swap_apps),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredSelectedApps, key = { it.packageName }, contentType = { "app" }) { app ->
                                AppRow(
                                    app = app,
                                    actionIcon = Icons.Default.Check,
                                    onClick = { onSelectedAppsChanged(selectedApps - app) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
