package com.blockads.vpn.ui.screens

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockads.vpn.R
import com.blockads.vpn.data.DnsProviders
import com.blockads.vpn.data.DnsStatsManager
import com.blockads.vpn.data.SettingsRepository
import com.blockads.vpn.service.VpnState
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

    val dnsProviderIp by settingsRepository.dnsProvider.collectAsState(initial = SettingsRepository.DEFAULT_DNS)

    var showDnsSheet by remember { mutableStateOf(false) }
    var showPauseSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
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
                        val providerNameResId = DnsProviders.getNameResByIp(dnsProviderIp)
                        val providerName = providerNameResId?.let { stringResource(it) } ?: stringResource(R.string.custom_dns)
                        CompanyAvatar(name = providerName)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = providerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
                currentIp = dnsProviderIp,
                onSelect = { ip ->
                    coroutineScope.launch {
                        settingsRepository.setDnsProvider(ip)
                        showDnsSheet = false
                    }
                },
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

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsSelectionSheetContent(
    currentIp: String,
    onSelect: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { },
            active = false,
            onActiveChange = { },
            modifier =
                Modifier
                    .fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_dns)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        ) {}

        Spacer(modifier = Modifier.height(16.dp))

        val filteredList =
            DnsProviders.providers.filter { provider ->
                val name = stringResource(provider.nameResId)
                name.contains(searchQuery, ignoreCase = true) || provider.ip.contains(searchQuery, ignoreCase = true)
            }

        val isCustomIp = android.util.Patterns.IP_ADDRESS.matcher(searchQuery).matches()

        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            if (isCustomIp && filteredList.none { it.ip == searchQuery }) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.custom_dns), fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(searchQuery) },
                        leadingContent = { CompanyAvatar(name = stringResource(R.string.custom)) },
                        modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).clickable { onSelect(searchQuery) },
                        colors =
                            ListItemDefaults.colors(
                                containerColor = if (searchQuery == currentIp) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            ),
                    )
                }
            }
            items(filteredList) { provider ->
                val name = stringResource(provider.nameResId)
                ListItem(
                    headlineContent = { Text(name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(provider.ip) },
                    leadingContent = { CompanyAvatar(name = name) },
                    modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).clickable { onSelect(provider.ip) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = if (provider.ip == currentIp) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        ),
                )
            }
        }
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
