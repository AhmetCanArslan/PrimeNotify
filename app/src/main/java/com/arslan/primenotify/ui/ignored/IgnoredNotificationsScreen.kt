package com.arslan.primenotify.ui.ignored

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arslan.primenotify.R
import com.arslan.primenotify.data.IgnoreRule
import com.arslan.primenotify.data.IgnoreType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoredNotificationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: IgnoredNotificationsViewModel = viewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val iconCache by viewModel.iconCache.collectAsState()
    var confirmRemoveRule by remember { mutableStateOf<IgnoreRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ignored_notifications_title)) },
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
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ignored_notifications_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
            ) {
                items(rules, key = { it.id }) { rule ->
                    IgnoredRuleItem(
                        rule = rule,
                        icon = iconCache[rule.packageName],
                        onRestoreClick = { confirmRemoveRule = rule }
                    )
                }
            }
        }
    }

    confirmRemoveRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { confirmRemoveRule = null },
            title = {
                Text(
                    text = stringResource(R.string.ignored_restore_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val desc = when {
                    rule.isRegex -> stringResource(R.string.ignored_restore_desc_pattern, rule.matchValue.orEmpty())
                    rule.type == IgnoreType.APP -> stringResource(R.string.ignored_restore_desc_app, rule.appName?.ifBlank { null } ?: rule.packageName)
                    rule.type == IgnoreType.TITLE -> stringResource(R.string.ignored_restore_desc_title, rule.matchValue.orEmpty())
                    rule.type == IgnoreType.BODY -> stringResource(R.string.ignored_restore_desc_body, rule.matchValue.orEmpty())
                    rule.type == IgnoreType.TITLE_AND_BODY -> stringResource(
                        R.string.ignored_restore_desc_title_and_body,
                        rule.matchValue.orEmpty(),
                        rule.matchValue2.orEmpty()
                    )
                    else -> ""
                }
                Text(desc)
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.removeRule(rule.id)
                    confirmRemoveRule = null
                }) {
                    Text(stringResource(R.string.ignored_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveRule = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun IgnoredRuleItem(
    rule: IgnoreRule,
    icon: ImageBitmap?,
    onRestoreClick: () -> Unit
) {
    val appDisplayName = rule.appName?.ifBlank { null } ?: rule.packageName

    val (typeLabel, badgeColor) = when {
        rule.isRegex || rule.isRegex2 -> Pair(
            stringResource(R.string.ignored_type_regex),
            MaterialTheme.colorScheme.error
        )
        rule.type == IgnoreType.APP -> Pair(
            stringResource(R.string.ignored_type_app),
            MaterialTheme.colorScheme.primary
        )
        rule.type == IgnoreType.TITLE -> Pair(
            stringResource(R.string.ignored_type_title),
            MaterialTheme.colorScheme.secondary
        )
        rule.type == IgnoreType.BODY -> Pair(
            stringResource(R.string.ignored_type_body),
            MaterialTheme.colorScheme.tertiary
        )
        rule.type == IgnoreType.TITLE_AND_BODY -> Pair(
            stringResource(R.string.ignored_type_title_and_body),
            MaterialTheme.colorScheme.secondary
        )
        else -> Pair("", MaterialTheme.colorScheme.primary)
    }

    val titleMatch = when (rule.type) {
        IgnoreType.TITLE, IgnoreType.TITLE_AND_BODY -> rule.matchValue?.takeIf { it.isNotBlank() }
        else -> null
    }
    val bodyMatch = when (rule.type) {
        IgnoreType.BODY -> rule.matchValue?.takeIf { it.isNotBlank() }
        IgnoreType.TITLE_AND_BODY -> rule.matchValue2?.takeIf { it.isNotBlank() }
        else -> null
    }
    val hasContent = titleMatch != null || bodyMatch != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {

            // ── Header row: icon | app name + subtitle | badge | restore ──
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
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appDisplayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // App name + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (rule.type) {
                            IgnoreType.APP -> stringResource(R.string.ignored_subtitle_all)
                            IgnoreType.TITLE -> stringResource(R.string.ignored_subtitle_title)
                            IgnoreType.BODY -> stringResource(R.string.ignored_subtitle_body)
                            IgnoreType.TITLE_AND_BODY -> stringResource(R.string.ignored_subtitle_title_and_body)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Type badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    contentColor = badgeColor
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Restore button
                IconButton(
                    onClick = onRestoreClick,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.ignored_restore_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Match values section ──
            if (hasContent) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (titleMatch != null) {
                        MatchRow(
                            label = stringResource(R.string.ignored_label_title),
                            value = titleMatch,
                            isRegex = rule.isRegex
                        )
                    }
                    if (bodyMatch != null) {
                        MatchRow(
                            label = stringResource(R.string.ignored_label_body),
                            value = bodyMatch,
                            isRegex = rule.isRegex2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchRow(label: String, value: String, isRegex: Boolean) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp)
        )
        Text(
            text = if (isRegex) value else "\"$value\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
