package com.blockads.app.ui.home

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blockads.app.domain.model.VpnState
import com.blockads.app.i18n.LocalStrings
import com.blockads.app.ui.components.PowerToggle
import com.blockads.app.ui.components.StatsCard
import com.blockads.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val vpnState by viewModel.vpnState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isPrivateDnsStrict by viewModel.isPrivateDnsStrict.collectAsState()
    val context = LocalContext.current

    val vpnPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.startVpn(context)
            }
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.lg))

            // Status chip
            AnimatedContent(
                targetState = vpnState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "statusChip",
            ) { state ->
                val (label, color) =
                    when (state) {
                        VpnState.ACTIVE -> strings.vpnActive to MaterialTheme.colorScheme.primary
                        VpnState.CONNECTING -> strings.vpnConnecting to MaterialTheme.colorScheme.secondary
                        VpnState.ERROR -> strings.vpnError to MaterialTheme.colorScheme.error
                        VpnState.STOPPED -> strings.vpnStopped to MaterialTheme.colorScheme.onSurfaceVariant
                        VpnState.PAUSED -> strings.pauseVpn to MaterialTheme.colorScheme.tertiary
                    }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                    colors =
                        AssistChipDefaults.assistChipColors(
                            disabledContainerColor = color.copy(alpha = 0.08f),
                            disabledLabelColor = color,
                            disabledLeadingIconContentColor = color,
                        ),
                    border =
                        AssistChipDefaults.assistChipBorder(
                            enabled = false,
                            disabledBorderColor = color.copy(alpha = 0.2f),
                        ),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            AnimatedVisibility(
                visible = isPrivateDnsStrict && vpnState == VpnState.ACTIVE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(strings.privateDnsWarningTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(strings.privateDnsWarningMessage, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Spacing.md))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(strings.privateDnsActionSettings)
                        }
                    }
                }
            }

            // Power toggle
            PowerToggle(
                vpnState = vpnState,
                onToggle = {
                    when (vpnState) {
                        VpnState.ACTIVE, VpnState.CONNECTING, VpnState.PAUSED -> viewModel.stopVpn(context)
                        VpnState.STOPPED, VpnState.ERROR -> {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnPermissionLauncher.launch(prepareIntent)
                            } else {
                                viewModel.startVpn(context)
                            }
                        }
                    }
                },
                contentDesc =
                    when (vpnState) {
                        VpnState.ACTIVE, VpnState.PAUSED -> strings.tapToStop
                        else -> strings.tapToStart
                    },
            )

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text =
                    when (vpnState) {
                        VpnState.ACTIVE, VpnState.PAUSED -> strings.tapToStop
                        VpnState.CONNECTING -> strings.vpnConnecting
                        VpnState.ERROR -> strings.vpnError
                        VpnState.STOPPED -> strings.tapToStart
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.weight(1f))

            // Stats card — only when active or paused
            AnimatedVisibility(
                visible = vpnState == VpnState.ACTIVE || vpnState == VpnState.PAUSED || stats.blockedCount > 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StatsCard(
                        stats = stats,
                        strings = strings,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.lg))
                }
            }

            // Pause / Resume UI
            AnimatedVisibility(
                visible = vpnState == VpnState.ACTIVE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                var showPauseMenu by remember { mutableStateOf(false) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        OutlinedButton(
                            onClick = { showPauseMenu = true },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.pauseVpn,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        DropdownMenu(
                            expanded = showPauseMenu,
                            onDismissRequest = { showPauseMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.pauseFor15m) },
                                onClick = {
                                    showPauseMenu = false
                                    viewModel.pauseVpn(context, 15 * 60 * 1000L)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(strings.pauseFor1h) },
                                onClick = {
                                    showPauseMenu = false
                                    viewModel.pauseVpn(context, 60 * 60 * 1000L)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                }
            }

            AnimatedVisibility(
                visible = vpnState == VpnState.PAUSED,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { viewModel.resumeVpn(context) },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.resumeVpn,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.height(Spacing.lg))
                }
            }

            // Current blocklist info
            settings?.let { s ->
                Text(
                    text = "${strings.currentBlocklist}: ${s.blocklistSource.key}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = Spacing.lg),
                )
            }
        }
    }
}
