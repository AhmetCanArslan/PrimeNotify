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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
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
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
    var rules by remember { mutableStateOf(rulesManager.getFlashRules()) }
    var showAddDialog by remember { mutableStateOf(false) }

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
            FloatingActionButton(onClick = { showAddDialog = true }) {
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

    if (showAddDialog) {
        val installedApps = remember { getInstalledApps(context) }
        AddFlashRuleDialog(
            apps = installedApps,
            onDismiss = { showAddDialog = false },
            onSave = { newRule ->
                rulesManager.addFlashRule(newRule)
                rules = rulesManager.getFlashRules()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RuleCard(
    rule: FlashRule,
    onToggle: (Boolean) -> Unit,
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
                    text = rule.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keyword: \"${rule.keyword}\"",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFlashRuleDialog(
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onSave: (FlashRule) -> Unit
) {
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var keyword by remember { mutableStateOf("") }
    var selectedPattern by remember { mutableStateOf(FlashPattern.HEARTBEAT) }

    var expandedApps by remember { mutableStateOf(false) }
    var expandedPatterns by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Flash Rule") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Selector
                ExposedDropdownMenuBox(
                    expanded = expandedApps,
                    onExpandedChange = { expandedApps = it }
                ) {
                    OutlinedTextField(
                        value = selectedApp?.name ?: "Select App",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Application") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedApps) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedApps,
                        onDismissRequest = { expandedApps = false }
                    ) {
                        apps.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.name) },
                                onClick = {
                                    selectedApp = app
                                    expandedApps = false
                                }
                            )
                        }
                    }
                }

                // Keyword Input
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Matching Word") },
                    placeholder = { Text("e.g. Mom") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Pattern Selector
                ExposedDropdownMenuBox(
                    expanded = expandedPatterns,
                    onExpandedChange = { expandedPatterns = it }
                ) {
                    OutlinedTextField(
                        value = selectedPattern.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Flash Pattern") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPatterns) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPatterns,
                        onDismissRequest = { expandedPatterns = false }
                    ) {
                        FlashPattern.entries.forEach { pattern ->
                            DropdownMenuItem(
                                text = { Text(pattern.displayName) },
                                onClick = {
                                    selectedPattern = pattern
                                    expandedPatterns = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val app = selectedApp
                    if (app != null && keyword.isNotBlank()) {
                        onSave(
                            FlashRule(
                                packageName = app.packageName,
                                appName = app.name,
                                keyword = keyword.trim(),
                                pattern = selectedPattern
                            )
                        )
                    }
                },
                enabled = selectedApp != null && keyword.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
