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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.R
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
                title = { Text(stringResource(R.string.flash_pattern_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
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
                        text = stringResource(R.string.rules),
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
                                text = stringResource(R.string.no_rules_configured),
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
                        Text(stringResource(R.string.add_new_rule))
                    }
                }

                // Patterns section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.custom_patterns),
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
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onNavigateToCreatePattern,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.create_custom_pattern))
                    }
                }
            }
        }
    }

    if (ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text(stringResource(R.string.delete_rule)) },
            text = { Text(stringResource(R.string.confirm_delete_rule)) },
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
                        Text(stringResource(R.string.delete))
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
            title = { Text(stringResource(R.string.delete_pattern)) },
            text = { Text(stringResource(R.string.confirm_delete_pattern, patternToDelete?.name ?: "")) },
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
                        Text(stringResource(R.string.delete))
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
                    text = if (rule.appNames.isNotEmpty()) rule.appNames.joinToString(", ") else stringResource(R.string.no_apps_chosen),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (rule.keywords.isEmpty()) stringResource(R.string.keywords_any) else stringResource(R.string.keywords_format, rule.keywords.joinToString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val patternText = if (rule.customPatternId != null) {
                    val cPattern = rulesManager.getCustomPatterns().find { it.id == rule.customPatternId }
                    stringResource(R.string.pattern_format, cPattern?.name ?: stringResource(R.string.unknown))
                } else stringResource(R.string.pattern_format, rule.pattern.displayName)
                val applyOnModes = mutableListOf<String>()
                if (rule.applyOnVibration) applyOnModes.add(stringResource(R.string.vibration))
                if (rule.applyOnSilent) applyOnModes.add(stringResource(R.string.silence))
                if (rule.applyOnDND) applyOnModes.add(stringResource(R.string.dnd))
                val modesText = if (applyOnModes.isEmpty()) stringResource(R.string.mode_none) else if (applyOnModes.size == 3) stringResource(R.string.mode_all) else applyOnModes.joinToString(", ")
                val silentModeText = stringResource(R.string.mode_on, modesText)
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
                        contentDescription = stringResource(R.string.edit_rule),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_rule_desc),
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


