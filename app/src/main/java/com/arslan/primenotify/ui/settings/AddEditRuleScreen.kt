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
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Warning
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
    val ignoreManager = remember { IgnoreManager(context) }
    val ignoreRules = remember { ignoreManager.getRules() }
    val hasProximitySensor = remember { rulesManager.hasProximitySensor() }

    // Draft key – "new" for a new rule, actual ruleId when editing
    val draftKey = ruleId?.takeIf { it != "new" } ?: "new"
    // Load any persisted draft (survives CreatePattern navigation and app backgrounding)
    val draft = remember { AddEditRuleDraft.load(context, draftKey) }

    // Consume prefill data (set from Logging screen) for new rules only
    val prefill = remember { if (ruleId == null || ruleId == "new") RulePrefillData.consume() else null }

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
    val initialScreenFlashAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.FLASH_SCREEN }
    }

    LaunchedEffect(Unit) {
        AppListManager.refresh(context)
    }

    val installedApps by AppListManager.installedApps.collectAsState()
    var selectedApps by remember(installedApps) {
        mutableStateOf(
            when {
                draft != null -> installedApps.filter { it.packageName in draft.selectedPackageNames }
                initialRule != null -> installedApps.filter { initialRule.packageNames.contains(it.packageName) }
                prefill != null -> installedApps.filter { it.packageName == prefill.packageName }
                else -> emptyList()
            }
        )
    }

    // Title & Body Keywords (new; legacy `keywords` pre-populated into title for migration)
    var titleKeywords by remember {
        mutableStateOf(
            draft?.titleKeywords
                ?: initialRule?.titleKeywords?.ifEmpty { initialRule.keywords }
                ?: prefill?.titleKeyword?.let { listOf(it) }
                ?: emptyList()
        )
    }
    var currentTitleKeyword by remember { mutableStateOf(draft?.currentTitleKeyword ?: "") }
    var bodyKeywords by remember {
        mutableStateOf(
            draft?.bodyKeywords
                ?: initialRule?.bodyKeywords
                ?: prefill?.bodyKeyword?.let { listOf(it) }
                ?: emptyList()
        )
    }
    var currentBodyKeyword by remember { mutableStateOf(draft?.currentBodyKeyword ?: "") }

    // Flash action state
    var customPatterns by remember { mutableStateOf(rulesManager.getCustomPatterns()) }
    var patternToDelete by remember { mutableStateOf<com.arslan.primenotify.data.CustomPattern?>(null) }
    var flashEnabled by remember { mutableStateOf(draft?.flashEnabled ?: (initialFlashAction != null)) }
    var flashPattern by remember {
        mutableStateOf(
            draft?.flashPattern?.let { runCatching { FlashPattern.valueOf(it) }.getOrNull() }
                ?: initialFlashAction?.flashPattern
                ?: FlashPattern.HEARTBEAT
        )
    }
    var flashCustomPatternId by remember {
        mutableStateOf(draft?.flashCustomPatternId ?: initialFlashAction?.customPatternId)
    }
    var expandedFlashPatterns by remember { mutableStateOf(false) }

    // WakeUp action state
    var wakeUpEnabled by remember { mutableStateOf(draft?.wakeUpEnabled ?: (initialWakeUpAction != null)) }
    var screenDurationSeconds by remember {
        mutableIntStateOf(draft?.screenDurationSeconds ?: initialWakeUpAction?.screenDurationSeconds ?: 10)
    }
    var pocketModeEnabled by remember {
        mutableStateOf(draft?.pocketModeEnabled ?: initialWakeUpAction?.pocketModeEnabled ?: true)
    }
    var expandedWakeUpDuration by remember { mutableStateOf(false) }

    // AOD action state
    var aodEnabled by remember { mutableStateOf(draft?.aodEnabled ?: (initialAodAction != null)) }
    var aodDurationSeconds by remember {
        mutableIntStateOf(draft?.aodDurationSeconds ?: initialAodAction?.aodDurationSeconds ?: 10)
    }
    var expandedAodDuration by remember { mutableStateOf(false) }

    // Flash Screen action state
    var screenFlashEnabled by remember {
        mutableStateOf(draft?.screenFlashEnabled ?: (initialScreenFlashAction != null))
    }
    var screenFlashColor by remember {
        mutableStateOf(
            draft?.screenFlashColor?.let { runCatching { ScreenFlashColor.valueOf(it) }.getOrNull() }
                ?: initialScreenFlashAction?.screenFlashColor?.let { runCatching { ScreenFlashColor.valueOf(it) }.getOrNull() }
                ?: ScreenFlashColor.RED
        )
    }
    var screenFlashDurationSeconds by remember {
        mutableIntStateOf(draft?.screenFlashDurationSeconds ?: initialScreenFlashAction?.screenFlashDurationSeconds ?: 5)
    }
    var expandedScreenFlashDuration by remember { mutableStateOf(false) }

    // Shared conditions
    var applyOnVibration by remember {
        mutableStateOf(draft?.applyOnVibration ?: initialRule?.applyOnVibration ?: true)
    }
    var applyOnSilent by remember {
        mutableStateOf(draft?.applyOnSilent ?: initialRule?.applyOnSilent ?: true)
    }
    var applyOnDND by remember {
        mutableStateOf(draft?.applyOnDND ?: initialRule?.applyOnDND ?: true)
    }
    var preventMultipleNotifications by remember {
        mutableStateOf(draft?.preventMultipleNotifications ?: initialRule?.preventMultipleNotifications ?: false)
    }

    val wakeUpDurationOptions = listOf(0, 5, 10, 15, 30, 60)
    val aodDurationOptions = listOf(-1, -2, 5, 10, 15, 30, 60, 120, 300)
    val screenFlashDurationOptions = listOf(5, 10, 30, 60, -1)

    val atLeastOneAction = flashEnabled || wakeUpEnabled || aodEnabled || screenFlashEnabled

    // Detect conflicts with existing ignore rules
    val ignoreConflicts by remember(selectedApps, titleKeywords, bodyKeywords) {
        derivedStateOf {
            val conflicts = mutableListOf<String>()
            for (app in selectedApps) {
                val pkg = app.packageName
                val appName = app.name
                val appRules = ignoreRules.filter { it.packageName == pkg }
                if (appRules.isEmpty()) continue
                // Entire app is muted
                if (appRules.any { it.type == IgnoreType.APP }) {
                    conflicts.add("🔕 All \"$appName\" notifications are muted")
                    continue
                }
                // Title keyword clashes
                for (kw in titleKeywords) {
                    val hit = appRules.any { rule ->
                        (rule.type == IgnoreType.TITLE || rule.type == IgnoreType.TITLE_AND_BODY) &&
                            !rule.matchValue.isNullOrBlank() &&
                            ignorePhraseMatches(kw, rule.matchValue, rule.isRegex)
                    }
                    if (hit) conflicts.add("🔕 Title keyword \"$kw\" in \"$appName\" is muted")
                }
                // Body keyword clashes
                for (kw in bodyKeywords) {
                    val hit = appRules.any { rule ->
                        (rule.type == IgnoreType.BODY &&
                            !rule.matchValue.isNullOrBlank() &&
                            ignorePhraseMatches(kw, rule.matchValue, rule.isRegex)) ||
                        (rule.type == IgnoreType.TITLE_AND_BODY &&
                            !rule.matchValue2.isNullOrBlank() &&
                            ignorePhraseMatches(kw, rule.matchValue2, rule.isRegex2))
                    }
                    if (hit) conflicts.add("🔕 Body keyword \"$kw\" in \"$appName\" is muted")
                }
            }
            conflicts
        }
    }

    // Keep a snapshot of the current form state so onDispose can persist it.
    // SideEffect runs after every successful recomposition, always capturing latest values.
    var latestDraft by remember { mutableStateOf<AddEditRuleDraft.Draft?>(null) }
    SideEffect {
        latestDraft = AddEditRuleDraft.Draft(
            ruleId = draftKey,
            selectedPackageNames = selectedApps.map { it.packageName },
            titleKeywords = titleKeywords,
            currentTitleKeyword = currentTitleKeyword,
            bodyKeywords = bodyKeywords,
            currentBodyKeyword = currentBodyKeyword,
            flashEnabled = flashEnabled,
            flashPattern = flashPattern.name,
            flashCustomPatternId = flashCustomPatternId,
            wakeUpEnabled = wakeUpEnabled,
            screenDurationSeconds = screenDurationSeconds,
            pocketModeEnabled = pocketModeEnabled,
            aodEnabled = aodEnabled,
            aodDurationSeconds = aodDurationSeconds,
            screenFlashEnabled = screenFlashEnabled,
            screenFlashColor = screenFlashColor.name,
            screenFlashDurationSeconds = screenFlashDurationSeconds,
            applyOnVibration = applyOnVibration,
            applyOnSilent = applyOnSilent,
            applyOnDND = applyOnDND,
            preventMultipleNotifications = preventMultipleNotifications,
        )
    }
    // Flag set to true when the user explicitly navigates back (discarding edits).
    // Uses a stable reference wrapper so onDispose can read the final value.
    val shouldDiscardDraft = remember { booleanArrayOf(false) }

    // Intercept the system/gesture back button to discard the draft.
    BackHandler {
        shouldDiscardDraft[0] = true
        AddEditRuleDraft.clear(context, draftKey)
        onNavigateBack()
    }

    // Save the draft whenever this composable leaves composition
    // BUT only when navigating FORWARD (e.g. to CreatePattern).
    // When the user navigates back (discard intent) we skip the save so
    // stale data can never pollute the next editing session.
    DisposableEffect(Unit) {
        onDispose {
            if (!shouldDiscardDraft[0]) {
                latestDraft?.let { AddEditRuleDraft.save(context, it) }
            }
        }
    }

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
                    IconButton(onClick = {
                        shouldDiscardDraft[0] = true
                        AddEditRuleDraft.clear(context, draftKey)
                        onNavigateBack()
                    }) {
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
                                // Flush any partially-typed keywords that weren't committed via Enter/+
                                val pendingTitle = currentTitleKeyword.trim()
                                val finalTitleKeywords = if (pendingTitle.isNotBlank() && !titleKeywords.contains(pendingTitle))
                                    titleKeywords + pendingTitle else titleKeywords
                                val pendingBody = currentBodyKeyword.trim()
                                val finalBodyKeywords = if (pendingBody.isNotBlank() && !bodyKeywords.contains(pendingBody))
                                    bodyKeywords + pendingBody else bodyKeywords

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
                                    if (screenFlashEnabled)
                                        add(RuleAction.flashScreen(screenFlashColor, screenFlashDurationSeconds))
                                }
                                val newRule = initialRule?.copy(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = emptyList(),
                                    titleKeywords = finalTitleKeywords,
                                    bodyKeywords = finalBodyKeywords,
                                    actions = actions,
                                    applyOnVibration = applyOnVibration,
                                    applyOnSilent = applyOnSilent,
                                    applyOnDND = applyOnDND,
                                    preventMultipleNotifications = preventMultipleNotifications
                                ) ?: NotificationRule(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = emptyList(),
                                    titleKeywords = finalTitleKeywords,
                                    bodyKeywords = finalBodyKeywords,
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
                                // Rule saved successfully – discard the draft and prevent
                                // onDispose from re-saving it.
                                shouldDiscardDraft[0] = true
                                AddEditRuleDraft.clear(context, draftKey)
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

                        // ── Title keywords ────────────────────────────────────
                        Text(
                            stringResource(R.string.trigger_title_keywords),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        AnimatedVisibility(
                            visible = titleKeywords.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(titleKeywords, key = { it }) { kw ->
                                    AssistChip(
                                        onClick = { titleKeywords = titleKeywords - kw },
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
                            value = currentTitleKeyword,
                            onValueChange = { currentTitleKeyword = it },
                            label = { Text(stringResource(R.string.add_title_keyword_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val kw = currentTitleKeyword.trim()
                                    if (kw.isNotBlank() && !titleKeywords.contains(kw)) {
                                        titleKeywords = titleKeywords + kw
                                        currentTitleKeyword = ""
                                    }
                                }
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val kw = currentTitleKeyword.trim()
                                        if (kw.isNotBlank() && !titleKeywords.contains(kw)) {
                                            titleKeywords = titleKeywords + kw
                                            currentTitleKeyword = ""
                                        }
                                    },
                                    enabled = currentTitleKeyword.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.cd_add)
                                    )
                                }
                            },
                            singleLine = true
                        )

                        HorizontalDivider()

                        // ── Body keywords ─────────────────────────────────────
                        Text(
                            stringResource(R.string.trigger_body_keywords),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        AnimatedVisibility(
                            visible = bodyKeywords.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(bodyKeywords, key = { it }) { kw ->
                                    AssistChip(
                                        onClick = { bodyKeywords = bodyKeywords - kw },
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
                            value = currentBodyKeyword,
                            onValueChange = { currentBodyKeyword = it },
                            label = { Text(stringResource(R.string.add_body_keyword_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val kw = currentBodyKeyword.trim()
                                    if (kw.isNotBlank() && !bodyKeywords.contains(kw)) {
                                        bodyKeywords = bodyKeywords + kw
                                        currentBodyKeyword = ""
                                    }
                                }
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val kw = currentBodyKeyword.trim()
                                        if (kw.isNotBlank() && !bodyKeywords.contains(kw)) {
                                            bodyKeywords = bodyKeywords + kw
                                            currentBodyKeyword = ""
                                        }
                                    },
                                    enabled = currentBodyKeyword.isNotBlank()
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

                // ── Ignore conflict warning ───────────────────────────────
                if (ignoreConflicts.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.rule_conflict_warning_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.rule_conflict_warning_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            ignoreConflicts.forEach { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
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

                        HorizontalDivider()

                        // Flash Screen action
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { screenFlashEnabled = !screenFlashEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.flash_screen_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = screenFlashEnabled,
                                    onCheckedChange = { screenFlashEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = screenFlashEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Colour grid – 12 preset colours, 4 per row
                                    Text(
                                        stringResource(R.string.flash_screen_color_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ScreenFlashColor.entries.chunked(4).forEach { row ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                row.forEach { color ->
                                                    val isSelected = screenFlashColor == color
                                                    Surface(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clickable { screenFlashColor = color },
                                                        shape = MaterialTheme.shapes.small,
                                                        color = androidx.compose.ui.graphics.Color(color.colorArgb),
                                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                                            3.dp,
                                                            MaterialTheme.colorScheme.onSurface
                                                        ) else null,
                                                        shadowElevation = if (isSelected) 4.dp else 0.dp
                                                    ) {}
                                                }
                                            }
                                        }
                                    }

                                    // Duration dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = expandedScreenFlashDuration,
                                        onExpandedChange = { expandedScreenFlashDuration = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val durationLabel = when (screenFlashDurationSeconds) {
                                            -1 -> stringResource(R.string.flash_screen_until_interaction)
                                            else -> stringResource(R.string.duration_seconds, screenFlashDurationSeconds)
                                        }
                                        OutlinedTextField(
                                            value = durationLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.flash_screen_duration_label)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScreenFlashDuration)
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedScreenFlashDuration,
                                            onDismissRequest = { expandedScreenFlashDuration = false }
                                        ) {
                                            screenFlashDurationOptions.forEach { sec ->
                                                val label = when (sec) {
                                                    -1 -> stringResource(R.string.flash_screen_until_interaction)
                                                    else -> stringResource(R.string.duration_seconds, sec)
                                                }
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        screenFlashDurationSeconds = sec
                                                        expandedScreenFlashDuration = false
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

/**
 * Returns true if [keyword] (as used in notification rule matching)
 * would be caught by an ignore rule whose pattern is [ignorePattern].
 * - Regex mode: pattern must match within keyword (containsMatchIn).
 * - Plain mode: keyword must equal pattern exactly (case-insensitive),
 *   matching how IgnoreManager.matchText works.
 */
private fun ignorePhraseMatches(keyword: String, ignorePattern: String, isRegex: Boolean): Boolean {
    return if (isRegex) {
        try {
            Regex(ignorePattern, RegexOption.IGNORE_CASE).containsMatchIn(keyword)
        } catch (_: Exception) { false }
    } else {
        keyword.equals(ignorePattern, ignoreCase = true)
    }
}