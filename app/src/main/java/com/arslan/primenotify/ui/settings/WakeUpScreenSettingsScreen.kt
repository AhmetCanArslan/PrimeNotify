package com.arslan.primenotify.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.data.RulesManager
import com.arslan.primenotify.data.WakeUpRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeUpScreenSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditRule: (String?) -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
    var rules by remember { mutableStateOf(rulesManager.getWakeUpRules()) }

    var ruleToDelete by remember { mutableStateOf<WakeUpRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wake Up Screen Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Rules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (rules.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No rules configured.\nTap below to add a new rule.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        WakeUpRuleCard(
                            rule = rule,
                            modifier = Modifier.animateItem(),
                            onToggle = { isEnabled ->
                                rulesManager.toggleWakeUpRule(rule.id, isEnabled)
                                rules = rulesManager.getWakeUpRules()
                            },
                            onEdit = {
                                onNavigateToAddEditRule(rule.id)
                            },
                            onDelete = {
                                ruleToDelete = rule
                            }
                        )
                    }
                }

                item {
                    Button(
                        onClick = { onNavigateToAddEditRule(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add New Rule")
                    }
                }
            }
        }
    }

    if (ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Rule") },
            text = { Text("Are you sure you want to delete this rule?") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = ruleToDelete
                        if (toDelete != null) {
                            rulesManager.removeWakeUpRule(toDelete.id)
                            rules = rulesManager.getWakeUpRules()
                        }
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WakeUpRuleCard(
    rule: WakeUpRule,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                val applyOnModes = mutableListOf<String>()
                if (rule.applyOnVibration) applyOnModes.add("Vib")
                if (rule.applyOnSilent) applyOnModes.add("Sil")
                if (rule.applyOnDND) applyOnModes.add("DND")
                val modesText = if (applyOnModes.isEmpty()) "None" else if (applyOnModes.size == 3) "All" else applyOnModes.joinToString(", ")
                val silentModeText = " · On: $modesText"
                Text(
                    text = "Duration: ${if (rule.screenDurationSeconds == 0) "Default" else "${rule.screenDurationSeconds}s"} · Pocket: ${if (rule.pocketModeEnabled) "On" else "Off"}$silentModeText",
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
