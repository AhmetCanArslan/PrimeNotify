package com.arslan.primenotify.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.R
import com.arslan.primenotify.data.RulesManager
import com.arslan.primenotify.data.AodRule
import com.arslan.primenotify.data.AppListManager
import com.arslan.primenotify.data.AppSelectionTable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAodRuleScreen(
    ruleId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val rulesManager = remember { RulesManager(context) }
    val initialRule = remember(ruleId) {
        if (ruleId != null && ruleId != "new") {
            rulesManager.getAodRules().find { it.id == ruleId }
        } else null
    }

    val installedApps by AppListManager.installedApps.collectAsState()
    var selectedApps by remember(initialRule, installedApps) {
        mutableStateOf(
            if (initialRule != null) installedApps.filter { initialRule.packageNames.contains(it.packageName) }
            else emptyList()
        )
    }

    var keywords by remember(initialRule) { mutableStateOf(initialRule?.keywords ?: emptyList()) }
    var currentKeyword by remember { mutableStateOf("") }

    var durationSeconds by remember(initialRule) { mutableIntStateOf(initialRule?.durationSeconds ?: 10) }

    var applyOnVibration by remember(initialRule) { mutableStateOf(initialRule?.applyOnVibration ?: true) }
    var applyOnSilent by remember(initialRule) { mutableStateOf(initialRule?.applyOnSilent ?: true) }
    var applyOnDND by remember(initialRule) { mutableStateOf(initialRule?.applyOnDND ?: true) }


    val durationOptions = listOf(-1, -2, 5, 10, 15, 30, 60, 120, 300)
    var expandedDuration by remember { mutableStateOf(false) }

    val formatDuration: (Int) -> String = { seconds ->
        when (seconds) {
            -1 -> "Until dismiss notification"
            -2 -> "Until unlocking the phone"
            else -> "${seconds}s"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialRule != null) stringResource(R.string.edit_aod_rule_title) else stringResource(R.string.create_aod_rule_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (selectedApps.isNotEmpty()) {
                                val newRule = initialRule?.copy(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = keywords,
                                    durationSeconds = durationSeconds,
                                    applyOnVibration = applyOnVibration,
                                    applyOnSilent = applyOnSilent,
                                    applyOnDND = applyOnDND
                                ) ?: AodRule(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = keywords,
                                    durationSeconds = durationSeconds,
                                    applyOnVibration = applyOnVibration,
                                    applyOnSilent = applyOnSilent,
                                    applyOnDND = applyOnDND
                                )

                                if (initialRule != null) {
                                    rulesManager.updateAodRule(newRule)
                                } else {
                                    rulesManager.addAodRule(newRule)
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = selectedApps.isNotEmpty(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Keyword Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.trigger_keywords), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    AnimatedVisibility(
                        visible = keywords.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(keywords, key = { it }) { kw ->
                                AssistChip(
                                    onClick = { keywords = keywords - kw },
                                    label = { Text(kw) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove)) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = currentKeyword,
                        onValueChange = { currentKeyword = it },
                        label = { Text(stringResource(R.string.add_keyword_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val kw = currentKeyword.trim()
                                if (kw.isNotBlank() && !keywords.contains(kw)) {
                                    keywords = keywords + kw
                                    currentKeyword = ""
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val kw = currentKeyword.trim()
                                    if (kw.isNotBlank() && !keywords.contains(kw)) {
                                        keywords = keywords + kw
                                        currentKeyword = ""
                                    }
                                },
                                enabled = currentKeyword.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
                            }
                        }
                    )
                }
            }

            // AOD Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.always_on_display_settings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    ExposedDropdownMenuBox(
                        expanded = expandedDuration,
                        onExpandedChange = { expandedDuration = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = formatDuration(durationSeconds),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.aod_duration)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDuration) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDuration,
                            onDismissRequest = { expandedDuration = false }
                        ) {
                            durationOptions.forEach { seconds ->
                                DropdownMenuItem(
                                    text = { Text(formatDuration(seconds)) },
                                    onClick = {
                                        durationSeconds = seconds
                                        expandedDuration = false
                                    }
                                )
                            }
                        }
                    }

                    Text(stringResource(R.string.apply_rule_on), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { applyOnVibration = !applyOnVibration }
                        ) {
                            Checkbox(checked = applyOnVibration, onCheckedChange = { applyOnVibration = it })
                            Text(stringResource(R.string.vibration), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { applyOnSilent = !applyOnSilent }
                        ) {
                            Checkbox(checked = applyOnSilent, onCheckedChange = { applyOnSilent = it })
                            Text(stringResource(R.string.silence), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { applyOnDND = !applyOnDND }
                        ) {
                            Checkbox(checked = applyOnDND, onCheckedChange = { applyOnDND = it })
                            Text(stringResource(R.string.dnd), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
            }

            // Apps Table Section
            AppSelectionTable(
                installedApps = installedApps,
                selectedApps = selectedApps,
                onSelectedAppsChanged = { selectedApps = it },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
