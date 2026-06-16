package com.adsblock.vpn.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adsblock.vpn.R
import com.adsblock.vpn.data.DnsLogManager
import com.adsblock.vpn.data.DnsProviders
import com.adsblock.vpn.data.DnsStatsManager
import com.adsblock.vpn.data.SettingsRepository
import com.adsblock.vpn.service.VpnState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settingsRepository: SettingsRepository,
    vpnState: VpnState,
    onToggleVpn: (Boolean) -> Unit,
    onPauseVpn: (Long) -> Unit,
    onResumeVpn: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val isConnected = vpnState != VpnState.DISCONNECTED
    val isPaused = vpnState == VpnState.PAUSED

    val dnsProviderId by settingsRepository.dnsProviderId.collectAsState(initial = SettingsRepository.DEFAULT_DNS_ID)
    val dnsProtocol by settingsRepository.dnsProtocol.collectAsState(initial = SettingsRepository.DEFAULT_PROTOCOL)

    var showDnsSheet by remember { mutableStateOf(false) }
    var showPauseSheet by remember { mutableStateOf(false) }

    val scrollState = androidx.compose.foundation.rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val statusText =
            when (vpnState) {
                VpnState.CONNECTED -> stringResource(R.string.vpn_connected)
                VpnState.PAUSED -> stringResource(R.string.notification_paused_title)
                VpnState.DISCONNECTED -> stringResource(R.string.vpn_disconnected)
            }

        val statusColor by animateColorAsState(
            targetValue =
                when (vpnState) {
                    VpnState.CONNECTED -> MaterialTheme.colorScheme.primary
                    VpnState.PAUSED -> Color(0xFFF59E0B) // Amber for paused
                    VpnState.DISCONNECTED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                },
            animationSpec = tween(500),
            label = "ShieldColor",
        )

        val buttonGradient =
            Brush.linearGradient(
                colors =
                    when (vpnState) {
                        VpnState.CONNECTED -> listOf(Color(0xFF10B981), Color(0xFF047857))
                        VpnState.PAUSED -> listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                        VpnState.DISCONNECTED -> listOf(Color.DarkGray, Color.Gray)
                    },
            )

        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
        val pulseScale1 by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (vpnState == VpnState.CONNECTED) 1.3f else 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale1",
        )
        val pulseScale2 by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (vpnState == VpnState.CONNECTED) 1.6f else 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale2",
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (vpnState == VpnState.CONNECTED) 0.2f else 0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseAlpha",
        )

        // Shield Button
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isConnected && !isPaused) {
                // Outer ripple
                Box(
                    modifier =
                        Modifier
                            .size(160.dp)
                            .scale(pulseScale2)
                            .background(statusColor.copy(alpha = pulseAlpha * 0.5f), shape = CircleShape),
                )
                // Inner ripple
                Box(
                    modifier =
                        Modifier
                            .size(160.dp)
                            .scale(pulseScale1)
                            .background(statusColor.copy(alpha = pulseAlpha), shape = CircleShape),
                )
            }
            // Main Button Surface
            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .shadow(if (isConnected) 24.dp else 8.dp, CircleShape, spotColor = statusColor)
                        .clip(CircleShape)
                        .background(buttonGradient)
                        .clickable {
                            if (isConnected) {
                                onToggleVpn(false)
                            } else {
                                DnsStatsManager.reset()
                                onToggleVpn(true)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.desc_vpn_shield),
                    modifier = Modifier.size(80.dp),
                    tint = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text =
                if (isConnected) {
                    stringResource(
                        R.string.tap_to_disconnect,
                    ).uppercase()
                } else {
                    stringResource(R.string.tap_to_connect).uppercase()
                },
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
        )

        if (isConnected) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.Warning else Icons.Default.Lock,
                        contentDescription = stringResource(R.string.desc_secured),
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) stringResource(R.string.status_connection_paused) else if (dnsProtocol == "doh") stringResource(R.string.status_secured_doh) else stringResource(R.string.status_secured_dot),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isPaused) {
                        onResumeVpn()
                    } else {
                        showPauseSheet = true
                    }
                },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isPaused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                Text(text = if (isPaused) stringResource(R.string.btn_resume) else stringResource(R.string.btn_pause))
            }
        } else {
            Spacer(modifier = Modifier.height(64.dp))
        }

        val logs by DnsLogManager.logs.collectAsState()

        if (isConnected && !isPaused) {
            Spacer(modifier = Modifier.height(24.dp))
            val recentLogs = logs.take(3)
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(12.dp)),
                color = Color(0xFF0F172A), // Dark slate
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FF41), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.live_threat_monitor),
                            color = Color(0xFF00FF41),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (recentLogs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.listening_traffic),
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        recentLogs.forEach { log ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "> ${log.domain.take(25)}${if (log.domain.length > 25) "..." else ""}",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = if (log.isBlocked) stringResource(R.string.status_blocked) else stringResource(R.string.status_allowed),
                                    color = if (log.isBlocked) Color(0xFFEF4444) else Color(0xFF00FF41),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val totalQueries by DnsStatsManager.totalQueries.collectAsState()
        val blockedQueries by DnsStatsManager.blockedQueries.collectAsState()

        // Stats Dashboard
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.stats_queries),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$totalQueries",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.stats_blocked),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$blockedQueries",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFEF4444),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // DNS Selection Card
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showDnsSheet = true },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(20.dp),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.setting_dns_provider),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val provider = DnsProviders.getProviderById(dnsProviderId)
                        val providerName = provider?.name ?: stringResource(R.string.custom_dns)
                        CompanyAvatar(name = providerName)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = providerName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (dnsProtocol == "doh") stringResource(R.string.protocol_doh) else stringResource(R.string.protocol_dot),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.desc_select_dns),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }

    if (showDnsSheet) {
        ModalBottomSheet(onDismissRequest = { showDnsSheet = false }) {
            DnsSelectionSheetContent(
                currentId = dnsProviderId,
                currentProtocol = dnsProtocol,
                onSelectId = { id ->
                    coroutineScope.launch {
                        settingsRepository.setDnsProviderId(id)
                        showDnsSheet = false
                    }
                },
                onSelectProtocol = { protocol ->
                    coroutineScope.launch {
                        settingsRepository.setDnsProtocol(protocol)
                    }
                }
            )
        }
    }

    if (showPauseSheet) {
        ModalBottomSheet(onDismissRequest = { showPauseSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.btn_pause), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    onPauseVpn(5)
                    showPauseSheet = false
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = stringResource(R.string.pause_duration_5m))
                }
                Button(onClick = {
                    onPauseVpn(15)
                    showPauseSheet = false
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = stringResource(R.string.pause_duration_15m))
                }
                Button(onClick = {
                    onPauseVpn(60)
                    showPauseSheet = false
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = stringResource(R.string.pause_duration_1h))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsSelectionSheetContent(
    currentId: String,
    currentProtocol: String,
    onSelectId: (String) -> Unit,
    onSelectProtocol: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var providerPrivacyDialog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Protocol Toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            val options = listOf("doh" to stringResource(R.string.protocol_doh), "dot" to stringResource(R.string.protocol_dot))
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = currentProtocol == value,
                    onClick = { onSelectProtocol(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(label)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_dns)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(50)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val filteredList = DnsProviders.providers.filter { provider ->
            provider.name.contains(searchQuery, ignoreCase = true) || provider.id.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            items(filteredList) { provider ->
                ListItem(
                    headlineContent = { Text(provider.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(if (currentProtocol == "doh") provider.dohUrl else provider.dotIp) },
                    leadingContent = { CompanyAvatar(name = provider.name) },
                    trailingContent = {
                        if (provider.privacyUrl.isNotEmpty()) {
                            IconButton(onClick = {
                                providerPrivacyDialog = provider.privacyUrl
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Info, 
                                    contentDescription = stringResource(R.string.title_privacy_policy),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).clickable { onSelectId(provider.id) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = if (provider.id == currentId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        ),
                )
            }
        }
    }

    if (providerPrivacyDialog != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { providerPrivacyDialog = null },
            title = { Text(stringResource(R.string.title_privacy_policy)) },
            text = { Text(stringResource(R.string.desc_privacy_redirect)) },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(providerPrivacyDialog))
                    context.startActivity(intent)
                    providerPrivacyDialog = null
                }) { Text(stringResource(R.string.btn_continue)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { providerPrivacyDialog = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun CompanyAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val hue = Math.abs(name.hashCode()) % 360f
    val color = Color.hsv(hue, 0.6f, 0.8f)

    Box(
        modifier =
            modifier
                .size(40.dp)
                .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}
