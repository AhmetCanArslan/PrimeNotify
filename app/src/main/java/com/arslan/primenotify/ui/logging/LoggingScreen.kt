package com.arslan.primenotify.ui.logging

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arslan.primenotify.R
import com.arslan.primenotify.data.LogEntry
import com.arslan.primenotify.data.MatchedRuleInfo
import com.arslan.primenotify.data.RuleType
import android.content.Context
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggingScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoggingViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val iconCache by viewModel.iconCache.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text(stringResource(R.string.logs_clear))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text(stringResource(R.string.logs_filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == RuleType.FLASH,
                        onClick = { viewModel.setFilter(RuleType.FLASH) },
                        label = { Text(stringResource(R.string.logs_filter_flash)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == RuleType.WAKE_UP,
                        onClick = { viewModel.setFilter(RuleType.WAKE_UP) },
                        label = { Text(stringResource(R.string.logs_filter_wake_up)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == RuleType.AOD,
                        onClick = { viewModel.setFilter(RuleType.AOD) },
                        label = { Text(stringResource(R.string.logs_filter_aod)) }
                    )
                }
            }

            if (logs.isEmpty()) {
                EmptyLogsState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(logs, key = { it.id }) { entry ->
                        LogItem(entry = entry, icon = iconCache[entry.packageName])
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.logs_clear_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.logs_clear_confirm_body, logs.size))
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearLogs()
                    showClearDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogItem(entry: LogEntry, icon: ImageBitmap?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: icon + app name + timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App icon
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Fallback: coloured circle with first letter
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.appName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(context, entry.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notification title + body
            if (entry.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.body.isNotBlank()) {
                Text(
                    text = entry.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Matched rule badges
            if (entry.matchedRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.matchedRules.forEach { rule ->
                        RuleBadge(rule = rule)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleBadge(rule: MatchedRuleInfo) {
    val (typeLabel, typeColor) = when (rule.ruleType) {
        RuleType.FLASH -> Pair(stringResource(R.string.logs_filter_flash), MaterialTheme.colorScheme.tertiary)
        RuleType.WAKE_UP -> Pair(stringResource(R.string.logs_filter_wake_up), MaterialTheme.colorScheme.secondary)
        RuleType.AOD -> Pair(stringResource(R.string.logs_filter_aod), MaterialTheme.colorScheme.primary)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = typeColor.copy(alpha = 0.12f),
        contentColor = typeColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = rule.ruleName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = if (rule.wasExecuted) Icons.Default.CheckCircle else Icons.Default.Clear,
                contentDescription = if (rule.wasExecuted) {
                    stringResource(R.string.log_rule_executed)
                } else {
                    stringResource(R.string.log_rule_blocked)
                },
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun EmptyLogsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.logs_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

private fun formatTimestamp(context: Context, timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }

    val diffMs = now.timeInMillis - timestamp
    val diffMin = diffMs / 60_000
    val diffHours = diffMs / 3_600_000

    val timeFmt = android.text.format.DateFormat.getTimeFormat(context)

    return when {
        diffMin < 1 -> "Just now"
        diffMin < 60 -> "$diffMin min ago"
        diffHours < 24 && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) ->
            timeFmt.format(Date(timestamp))
        diffHours < 48 -> "Yesterday " + timeFmt.format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp)) +
            " " + timeFmt.format(Date(timestamp))
    }
}
