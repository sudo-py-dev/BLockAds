package com.blockads.app.ui.rules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blockads.app.core.data.rules.CustomRule
import com.blockads.app.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.customRulesTitle, fontWeight = FontWeight.Bold) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = strings.addRule)
            }
        },
    ) { padding ->
        if (rules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(strings.noRules, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(rules, key = { it.domain }) { rule ->
                    RuleItem(
                        rule = rule,
                        onDelete = { viewModel.removeRule(rule) },
                        strings = strings,
                    )
                }
            }
        }

        if (showAddDialog) {
            AddRuleDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { domain, isWhitelist ->
                    viewModel.addRule(domain, isWhitelist)
                    showAddDialog = false
                },
                strings = strings,
            )
        }
    }
}

@Composable
fun RuleItem(
    rule: CustomRule,
    onDelete: () -> Unit,
    strings: com.blockads.app.i18n.LocalizedStrings,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.domain,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (rule.isWhitelist) strings.whitelist else strings.blacklist,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rule.isWhitelist) Color(0xFF43A047) else Color(0xFFE53935),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (domain: String, isWhitelist: Boolean) -> Unit,
    strings: com.blockads.app.i18n.LocalizedStrings,
) {
    var domain by remember { mutableStateOf("") }
    var isWhitelist by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addRule) },
        text = {
            Column {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text(strings.domainHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isWhitelist,
                        onClick = { isWhitelist = false },
                    )
                    Text(strings.blacklist)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = isWhitelist,
                        onClick = { isWhitelist = true },
                    )
                    Text(strings.whitelist)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(domain, isWhitelist) },
                enabled = domain.isNotBlank(),
            ) {
                Text(strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}
