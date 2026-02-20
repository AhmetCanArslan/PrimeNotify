package com.arslan.primenotify.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.primenotify.data.FlashPattern
import com.arslan.primenotify.data.FlashRule
import com.arslan.primenotify.data.RulesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFlashRuleScreen(
    ruleId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val rulesManager = remember { RulesManager(context) }
    val initialRule = remember(ruleId) {
        if (ruleId != null && ruleId != "new") {
            rulesManager.getFlashRules().find { it.id == ruleId }
        } else null
    }

    val installedApps by produceState<List<AppItem>>(initialValue = emptyList()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            getInstalledApps(context)
        }
    }
    var selectedApps by remember(initialRule, installedApps) { 
        mutableStateOf(
            if (initialRule != null) installedApps.filter { initialRule.packageNames.contains(it.packageName) }
            else emptyList()
        ) 
    }
    
    var keywords by remember(initialRule) { mutableStateOf(initialRule?.keywords ?: emptyList()) }
    var currentKeyword by remember { mutableStateOf("") }
    
    val customPatterns = remember { rulesManager.getCustomPatterns() }
    var selectedStandardPattern by remember(initialRule) { mutableStateOf(initialRule?.pattern ?: FlashPattern.HEARTBEAT) }
    var selectedCustomPatternId by remember(initialRule) { mutableStateOf(initialRule?.customPatternId) }
    val isLoadingApps = installedApps.isEmpty()
    
    var expandedPatterns by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialRule != null) "Edit Rule" else "Create Rule") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                                    keyword = "",
                                    keywords = keywords,
                                    pattern = selectedStandardPattern,
                                    customPatternId = selectedCustomPatternId
                                ) ?: FlashRule(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keyword = "",
                                    keywords = keywords,
                                    pattern = selectedStandardPattern,
                                    customPatternId = selectedCustomPatternId
                                )
                                
                                if (initialRule != null) {
                                    rulesManager.updateFlashRule(newRule)
                                } else {
                                    rulesManager.addFlashRule(newRule)
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = selectedApps.isNotEmpty(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
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
                    Text("Trigger Keywords", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
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
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove") },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = currentKeyword,
                        onValueChange = { currentKeyword = it },
                        label = { Text("Add Keyword (Optional)") },
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
                                Icon(Icons.Default.Add, contentDescription = "Add Keyword")
                            }
                        }
                    )
                }
            }

            // Pattern Section
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
                    Text("Flash Pattern", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedPatterns,
                        onExpandedChange = { expandedPatterns = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (selectedCustomPatternId != null) {
                                customPatterns.find { it.id == selectedCustomPatternId }?.name ?: "Unknown Custom"
                            } else {
                                selectedStandardPattern.displayName
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Pattern") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPatterns) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPatterns,
                            onDismissRequest = { expandedPatterns = false }
                        ) {
                            FlashPattern.entries.forEach { pattern ->
                                DropdownMenuItem(
                                    text = { Text(pattern.displayName) },
                                    onClick = {
                                        selectedStandardPattern = pattern
                                        selectedCustomPatternId = null
                                        expandedPatterns = false
                                    }
                                )
                            }
                            if (customPatterns.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                customPatterns.forEach { cPattern ->
                                    DropdownMenuItem(
                                        text = { Text(cPattern.name) },
                                        onClick = {
                                            selectedCustomPatternId = cPattern.id
                                            expandedPatterns = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Apps Table Section
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Target Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val unselectedApps = installedApps.filter { it !in selectedApps }
                        
                        // Left Column
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Text("Available", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodyMedium)
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(unselectedApps, key = { it.packageName }) { app ->
                                    AppRow(
                                        app = app, 
                                        actionIcon = Icons.Default.Add,
                                        onClick = { selectedApps = selectedApps + app }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        VerticalDivider()
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Right Column
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Selected", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                IconButton(
                                    onClick = { selectedApps = installedApps.filter { it !in selectedApps } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Swap Apps",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(selectedApps, key = { it.packageName }) { app ->
                                    AppRow(
                                        app = app, 
                                        actionIcon = Icons.Default.Check,
                                        onClick = { selectedApps = selectedApps - app }
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
