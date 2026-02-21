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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.data.CustomPattern
import com.arslan.primenotify.data.FlashRule
import com.arslan.primenotify.data.RulesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditRule: (String?) -> Unit,
    onNavigateToCreatePattern: () -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
    var rules by remember { mutableStateOf(rulesManager.getFlashRules()) }
    var customPatterns by remember { mutableStateOf(rulesManager.getCustomPatterns()) }

    var ruleToDelete by remember { mutableStateOf<FlashRule?>(null) }
    var patternToDelete by remember { mutableStateOf<CustomPattern?>(null) }

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
                // Rules section
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
                                text = "No rules configured.\nTap + to add a new rule.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            modifier = Modifier.animateItem(),
                            onToggle = { isEnabled ->
                                rulesManager.toggleFlashRule(rule.id, isEnabled)
                                rules = rulesManager.getFlashRules()
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

                // Patterns section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Custom Patterns",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(customPatterns, key = { it.id }) { pattern ->
                    Card(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pattern.name, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = {
                                patternToDelete = pattern
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Pattern", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onNavigateToCreatePattern,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Custom Pattern")
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
                            rulesManager.removeFlashRule(toDelete.id)
                            rules = rulesManager.getFlashRules()
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

    if (patternToDelete != null) {
        AlertDialog(
            onDismissRequest = { patternToDelete = null },
            title = { Text("Delete Pattern") },
            text = { Text("Are you sure you want to delete pattern '${patternToDelete?.name}'?\nAny rules using it will reset to default.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = patternToDelete
                        if (toDelete != null) {
                            rulesManager.removeCustomPattern(toDelete.id)
                            customPatterns = rulesManager.getCustomPatterns()
                            
                            val currentRules = rulesManager.getFlashRules()
                            currentRules.filter { it.customPatternId == toDelete.id }.forEach {
                                rulesManager.updateFlashRule(it.copy(customPatternId = null))
                            }
                            rules = rulesManager.getFlashRules()
                        }
                        patternToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { patternToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RuleCard(
    rule: FlashRule,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
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
                val patternText = if (rule.customPatternId != null) {
                    val cPattern = rulesManager.getCustomPatterns().find { it.id == rule.customPatternId }
                    "Pattern: ${cPattern?.name ?: "Unknown"}"
                } else "Pattern: ${rule.pattern.displayName}"
                val applyOnModes = mutableListOf<String>()
                if (rule.applyOnVibration) applyOnModes.add("Vib")
                if (rule.applyOnSilent) applyOnModes.add("Sil")
                if (rule.applyOnDND) applyOnModes.add("DND")
                val modesText = if (applyOnModes.isEmpty()) "None" else if (applyOnModes.size == 3) "All" else applyOnModes.joinToString(", ")
                val silentModeText = " · On: $modesText"
                Text(
                    text = "$patternText$silentModeText",
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


