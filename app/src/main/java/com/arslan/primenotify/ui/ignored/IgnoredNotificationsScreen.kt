package com.arslan.primenotify.ui.ignored

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
                val desc = when (rule.type) {
                    IgnoreType.APP -> stringResource(R.string.ignored_restore_desc_app, rule.appName?.ifBlank { null } ?: rule.packageName)
                    IgnoreType.TITLE -> stringResource(R.string.ignored_restore_desc_title, rule.matchValue.orEmpty())
                    IgnoreType.BODY -> stringResource(R.string.ignored_restore_desc_body, rule.matchValue.orEmpty())
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
private fun IgnoredRuleItem(rule: IgnoreRule, onRestoreClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Type badge
            val (typeLabel, badgeColor) = when (rule.type) {
                IgnoreType.APP -> Pair(
                    stringResource(R.string.ignored_type_app),
                    MaterialTheme.colorScheme.primary
                )
                IgnoreType.TITLE -> Pair(
                    stringResource(R.string.ignored_type_title),
                    MaterialTheme.colorScheme.secondary
                )
                IgnoreType.BODY -> Pair(
                    stringResource(R.string.ignored_type_body),
                    MaterialTheme.colorScheme.tertiary
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor.copy(alpha = 0.12f),
                contentColor = badgeColor
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.appName?.ifBlank { null } ?: rule.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!rule.matchValue.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${rule.matchValue}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Restore (un-ignore) button
            IconButton(
                onClick = onRestoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.ignored_restore_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
