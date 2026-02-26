package com.arslan.primenotify.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.R
import com.arslan.primenotify.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleScreen(
    ruleId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToCreatePattern: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val rulesManager = remember { RulesManager(context) }
    val hasProximitySensor = remember { rulesManager.hasProximitySensor() }

    val initialRule = remember(ruleId) {
        if (ruleId != null && ruleId != "new") {
            rulesManager.getRules().find { it.id == ruleId }
        } else null
    }

    val initialFlashAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.FLASH }
    }
    val initialWakeUpAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.WAKE_UP }
    }
    val initialAodAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.AOD }
    }

    val installedApps by AppListManager.installedApps.collectAsState()
    var selectedApps by remember(initialRule, installedApps) {
        mutableStateOf(
            if (initialRule != null)
                installedApps.filter { initialRule.packageNames.contains(it.packageName) }
            else emptyList()
        )
    }

    // Keywords
    var keywords by remember(initialRule) { mutableStateOf(initialRule?.keywords ?: emptyList()) }
    var currentKeyword by remember { mutableStateOf("") }

    // Flash action state
    var customPatterns by remember { mutableStateOf(rulesManager.getCustomPatterns()) }
    var patternToDelete by remember { mutableStateOf<com.arslan.primenotify.data.CustomPattern?>(null) }
    var flashEnabled by remember(initialFlashAction) { mutableStateOf(initialFlashAction != null) }
    var flashPattern by remember(initialFlashAction) {
        mutableStateOf(initialFlashAction?.flashPattern ?: FlashPattern.HEARTBEAT)
    }
    var flashCustomPatternId by remember(initialFlashAction) {
        mutableStateOf(initialFlashAction?.customPatternId)
    }
    var expandedFlashPatterns by remember { mutableStateOf(false) }

    // WakeUp action state
    var wakeUpEnabled by remember(initialWakeUpAction) { mutableStateOf(initialWakeUpAction != null) }
    var screenDurationSeconds by remember(initialWakeUpAction) {
        mutableIntStateOf(initialWakeUpAction?.screenDurationSeconds ?: 10)
    }
    var pocketModeEnabled by remember(initialWakeUpAction) {
        mutableStateOf(initialWakeUpAction?.pocketModeEnabled ?: true)
    }
    var expandedWakeUpDuration by remember { mutableStateOf(false) }

    // AOD action state
    var aodEnabled by remember(initialAodAction) { mutableStateOf(initialAodAction != null) }
    var aodDurationSeconds by remember(initialAodAction) {
        mutableIntStateOf(initialAodAction?.aodDurationSeconds ?: 10)
    }
    var expandedAodDuration by remember { mutableStateOf(false) }

    // Shared conditions
    var applyOnVibration by remember(initialRule) {
        mutableStateOf(initialRule?.applyOnVibration ?: true)
    }
    var applyOnSilent by remember(initialRule) {
        mutableStateOf(initialRule?.applyOnSilent ?: true)
    }
    var applyOnDND by remember(initialRule) {
        mutableStateOf(initialRule?.applyOnDND ?: true)
    }
    var preventMultipleNotifications by remember(initialRule) {
        mutableStateOf(initialRule?.preventMultipleNotifications ?: false)
    }

    val wakeUpDurationOptions = listOf(0, 5, 10, 15, 30, 60)
    val aodDurationOptions = listOf(-1, -2, 5, 10, 15, 30, 60, 120, 300)

    val atLeastOneAction = flashEnabled || wakeUpEnabled || aodEnabled

    val consumeAllScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initialRule != null) stringResource(R.string.edit_rule_title)
                        else stringResource(R.string.create_rule_title)
                    )
                },
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
                            if (selectedApps.isNotEmpty() && atLeastOneAction) {
                                val actions = buildList {
                                    if (flashEnabled)
                                        add(RuleAction.flash(flashPattern, flashCustomPatternId))
                                    if (wakeUpEnabled)
                                        add(RuleAction.wakeUp(
                                            screenDurationSeconds,
                                            if (hasProximitySensor) pocketModeEnabled else false
                                        ))
                                    if (aodEnabled)
                                        add(RuleAction.aod(aodDurationSeconds))
                                }
                                val newRule = initialRule?.copy(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = keywords,
                                    actions = actions,
                                    applyOnVibration = applyOnVibration,
                                    applyOnSilent = applyOnSilent,
                                    applyOnDND = applyOnDND,
                                    preventMultipleNotifications = preventMultipleNotifications
                                ) ?: NotificationRule(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = keywords,
                                    actions = actions,
                                    applyOnVibration = applyOnVibration,
                                    applyOnSilent = applyOnSilent,
                                    applyOnDND = applyOnDND,
                                    preventMultipleNotifications = preventMultipleNotifications
                                )
                                if (initialRule != null) {
                                    rulesManager.updateRule(newRule)
                                } else {
                                    rulesManager.addRule(newRule)
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = selectedApps.isNotEmpty() && atLeastOneAction,
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // ── Keywords ──────────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.trigger_keywords),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AnimatedVisibility(
                            visible = keywords.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(keywords, key = { it }) { kw ->
                                    AssistChip(
                                        onClick = { keywords = keywords - kw },
                                        label = { Text(kw) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.cd_remove)
                                            )
                                        },
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
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.cd_add)
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }
                }

                // ── Actions ───────────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.actions_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Flash action
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { flashEnabled = !flashEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.flash_pattern_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = flashEnabled,
                                    onCheckedChange = { flashEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = flashEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedFlashPatterns,
                                        onExpandedChange = { expandedFlashPatterns = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val selectedLabel = if (flashCustomPatternId != null) {
                                            customPatterns.find { it.id == flashCustomPatternId }?.name
                                                ?: stringResource(R.string.unknown_custom)
                                        } else {
                                            flashPattern.displayName
                                        }
                                        OutlinedTextField(
                                            value = selectedLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.flash_pattern)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expandedFlashPatterns
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedFlashPatterns,
                                            onDismissRequest = { expandedFlashPatterns = false }
                                        ) {
                                            FlashPattern.entries.forEach { pattern ->
                                                DropdownMenuItem(
                                                    text = { Text(pattern.displayName) },
                                                    onClick = {
                                                        flashPattern = pattern
                                                        flashCustomPatternId = null
                                                        expandedFlashPatterns = false
                                                    }
                                                )
                                            }
                                            if (customPatterns.isNotEmpty()) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                                customPatterns.forEach { cPattern ->
                                                    DropdownMenuItem(
                                                        text = { Text(cPattern.name) },
                                                        onClick = {
                                                            flashCustomPatternId = cPattern.id
                                                            expandedFlashPatterns = false
                                                        },
                                                        trailingIcon = {
                                                            IconButton(
                                                                onClick = {
                                                                    expandedFlashPatterns = false
                                                                    patternToDelete = cPattern
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = stringResource(R.string.cd_delete),
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = onNavigateToCreatePattern,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.create_custom_pattern))
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Wake Up action
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { wakeUpEnabled = !wakeUpEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.wake_up_screen_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = wakeUpEnabled,
                                    onCheckedChange = { wakeUpEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = wakeUpEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedWakeUpDuration,
                                        onExpandedChange = { expandedWakeUpDuration = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val durLabel = if (screenDurationSeconds == 0)
                                            stringResource(R.string.default_duration)
                                        else
                                            stringResource(R.string.duration_format, screenDurationSeconds)
                                        OutlinedTextField(
                                            value = durLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.screen_duration)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expandedWakeUpDuration
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedWakeUpDuration,
                                            onDismissRequest = { expandedWakeUpDuration = false }
                                        ) {
                                            wakeUpDurationOptions.forEach { sec ->
                                                val label = if (sec == 0)
                                                    stringResource(R.string.default_duration)
                                                else
                                                    stringResource(R.string.duration_format, sec)
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        screenDurationSeconds = sec
                                                        expandedWakeUpDuration = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    // Pocket mode
                                    Row(
                                        modifier = if (hasProximitySensor)
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { pocketModeEnabled = !pocketModeEnabled }
                                        else Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = pocketModeEnabled && hasProximitySensor,
                                            onCheckedChange = { if (hasProximitySensor) pocketModeEnabled = it },
                                            enabled = hasProximitySensor
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.pocket_mode),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (hasProximitySensor)
                                                    MaterialTheme.colorScheme.onSurface
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                            Text(
                                                text = if (hasProximitySensor)
                                                    stringResource(R.string.pocket_mode_desc)
                                                else
                                                    stringResource(R.string.proximity_sensor_unavailable),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (hasProximitySensor)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // AOD action
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { aodEnabled = !aodEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.turn_on_aod_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = aodEnabled,
                                    onCheckedChange = { aodEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = aodEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                                ) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedAodDuration,
                                        onExpandedChange = { expandedAodDuration = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val aodLabel = when (aodDurationSeconds) {
                                            -1 -> stringResource(R.string.until_dismiss_notification)
                                            -2 -> stringResource(R.string.until_unlocking_phone)
                                            else -> stringResource(
                                                R.string.duration_seconds,
                                                aodDurationSeconds
                                            )
                                        }
                                        OutlinedTextField(
                                            value = aodLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.aod_duration)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expandedAodDuration
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedAodDuration,
                                            onDismissRequest = { expandedAodDuration = false }
                                        ) {
                                            aodDurationOptions.forEach { sec ->
                                                val label = when (sec) {
                                                    -1 -> stringResource(R.string.until_dismiss_notification)
                                                    -2 -> stringResource(R.string.until_unlocking_phone)
                                                    else -> stringResource(
                                                        R.string.duration_seconds,
                                                        sec
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        aodDurationSeconds = sec
                                                        expandedAodDuration = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Conditions ────────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.apply_rule_on),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.clickable { applyOnVibration = !applyOnVibration },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyOnVibration,
                                    onCheckedChange = { applyOnVibration = it }
                                )
                                Text(
                                    stringResource(R.string.vibration),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.clickable { applyOnSilent = !applyOnSilent },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyOnSilent,
                                    onCheckedChange = { applyOnSilent = it }
                                )
                                Text(
                                    stringResource(R.string.silence),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.clickable { applyOnDND = !applyOnDND },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyOnDND,
                                    onCheckedChange = { applyOnDND = it }
                                )
                                Text(
                                    stringResource(R.string.dnd),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preventMultipleNotifications = !preventMultipleNotifications
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = preventMultipleNotifications,
                                onCheckedChange = { preventMultipleNotifications = it }
                            )
                            Text(
                                stringResource(R.string.prevent_multiple_notifications),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

            // App selection
            AppSelectionTable(
                installedApps = installedApps,
                selectedApps = selectedApps,
                onSelectedAppsChanged = { selectedApps = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .nestedScroll(consumeAllScrollConnection)
            )
        }
    }

    // Delete custom pattern dialog
    patternToDelete?.let { pattern ->
        AlertDialog(
            onDismissRequest = { patternToDelete = null },
            title = {
                Text(
                    stringResource(R.string.delete_pattern),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.confirm_delete_pattern, pattern.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        rulesManager.removeCustomPattern(pattern.id)
                        if (flashCustomPatternId == pattern.id) {
                            flashCustomPatternId = null
                        }
                        customPatterns = rulesManager.getCustomPatterns()
                        patternToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { patternToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}