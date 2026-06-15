package com.blockads.app.ui.home

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
            TopAppBar(
                title = {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.xl))

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
                        Icon(Icons.Rounded.Shield, contentDescription = null)
                    },
                    colors =
                        AssistChipDefaults.assistChipColors(
                            disabledContainerColor = color.copy(alpha = 0.12f),
                            disabledLabelColor = color,
                            disabledLeadingIconContentColor = color,
                        ),
                    border =
                        AssistChipDefaults.assistChipBorder(
                            enabled = false,
                            disabledBorderColor = color.copy(alpha = 0.3f),
                        ),
                )
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

            Text(
                text =
                    when (vpnState) {
                        VpnState.ACTIVE, VpnState.PAUSED -> strings.tapToStop
                        VpnState.CONNECTING -> strings.vpnConnecting
                        VpnState.ERROR -> strings.vpnError
                        VpnState.STOPPED -> strings.tapToStart
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Pause / Resume UI
            if (vpnState == VpnState.ACTIVE) {
                var showPauseMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { showPauseMenu = true }) {
                        Icon(Icons.Rounded.Pause, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.pauseVpn)
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
            } else if (vpnState == VpnState.PAUSED) {
                Button(onClick = { viewModel.resumeVpn(context) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.resumeVpn)
                }
            }

            // Stats card — only when active or paused
            if (vpnState == VpnState.ACTIVE || vpnState == VpnState.PAUSED || stats.blockedCount > 0) {
                StatsCard(
                    stats = stats,
                    strings = strings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // Current blocklist info
            settings?.let { s ->
                Text(
                    text = "${strings.currentBlocklist}: ${s.blocklistSource.key}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
