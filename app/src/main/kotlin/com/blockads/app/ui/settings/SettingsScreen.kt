package com.blockads.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.blockads.app.core.data.blocklist.BlocklistSource
import com.blockads.app.domain.model.ThemeMode
import com.blockads.app.i18n.LocalStrings
import com.blockads.app.ui.components.AppDropdown
import com.blockads.app.ui.components.SectionHeader
import com.blockads.app.ui.components.SwitchRow
import com.blockads.app.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val IPV4_REGEX = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")
private val URL_REGEX = Regex("^https://.*")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val settings by viewModel.settings.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(refreshError) {
        if (refreshError != null) {
            snackbarHost.showSnackbar(strings.errorBlocklistDownload)
            viewModel.clearRefreshError()
        }
    }

    val allSources =
        remember {
            listOf(
                BlocklistSource.Bundled,
                BlocklistSource.AdGuardDns,
                BlocklistSource.AdGuardHosts,
                BlocklistSource.StevenBlack,
                BlocklistSource.Oisd,
                BlocklistSource.HaGeZiPro,
                BlocklistSource.CloudflareFamilies,
                BlocklistSource.Quad9,
                BlocklistSource.Custom(""),
            )
        }

    fun sourceLabel(src: BlocklistSource): String =
        when (src) {
            is BlocklistSource.Bundled -> strings.sourceBundled
            is BlocklistSource.AdGuardDns -> strings.sourceAdGuardDns
            is BlocklistSource.AdGuardHosts -> strings.sourceAdGuardHosts
            is BlocklistSource.StevenBlack -> strings.sourceStevenBlack
            is BlocklistSource.Oisd -> strings.sourceOisd
            is BlocklistSource.HaGeZiPro -> strings.sourceHaGeZiPro
            is BlocklistSource.CloudflareFamilies -> strings.sourceCloudflareFamilies
            is BlocklistSource.Quad9 -> strings.sourceQuad9
            is BlocklistSource.Custom -> strings.sourceCustom
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // ─── Appearance ─────────────────────────────────────────────
            item { SectionHeader(strings.appearanceSection) }

            item {
                Column(Modifier.padding(horizontal = Spacing.md)) {
                    AppDropdown(
                        label = strings.themeLabel,
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        displayName = { mode ->
                            when (mode) {
                                ThemeMode.LIGHT -> strings.themeLight
                                ThemeMode.DARK -> strings.themeDark
                                ThemeMode.SYSTEM -> strings.themeSystem
                            }
                        },
                        onSelect = viewModel::setTheme,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    val languageOptions =
                        remember {
                            listOf("" to strings.languageSystem) +
                                listOf(
                                    "en" to "English", "he" to "עברית",
                                    "ar" to "العربية", "fr" to "Français",
                                    "de" to "Deutsch", "es" to "Español",
                                )
                        }
                    AppDropdown(
                        label = strings.languageLabel,
                        options = languageOptions,
                        selected = languageOptions.first { it.first == settings.languageTag },
                        displayName = { it.second },
                        onSelect = { viewModel.setLanguage(it.first) },
                    )
                }
            }

            item { HorizontalDivider(Modifier.padding(top = Spacing.md)) }

            // ─── Ad Blocking ─────────────────────────────────────────────
            item { SectionHeader(strings.blocklistSection) }

            item {
                Column(Modifier.padding(horizontal = Spacing.md)) {
                    AppDropdown(
                        label = strings.blocklistLabel,
                        options = allSources,
                        selected = settings.blocklistSource,
                        displayName = ::sourceLabel,
                        onSelect = viewModel::setBlocklistSource,
                    )

                    AnimatedVisibility(visible = settings.blocklistSource is BlocklistSource.Custom) {
                        var urlInput by rememberSaveable { mutableStateOf(settings.customBlocklistUrl) }
                        val isValidUrl = urlInput.isEmpty() || URL_REGEX.matches(urlInput)
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                viewModel.setCustomUrl(it)
                            },
                            label = { Text(strings.customUrlLabel) },
                            placeholder = { Text(strings.customUrlHint) },
                            isError = !isValidUrl,
                            supportingText =
                                if (!isValidUrl) {
                                    { Text(strings.errorInvalidUrl, color = MaterialTheme.colorScheme.error) }
                                } else {
                                    null
                                },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                        )
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    val lastUpdated =
                        remember(settings.blocklistLastUpdatedMs) {
                            if (settings.blocklistLastUpdatedMs > 0L) {
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                                    .format(Date(settings.blocklistLastUpdatedMs))
                            } else {
                                null
                            }
                        }
                    if (lastUpdated != null) {
                        Text(
                            text = "${strings.lastUpdated}: $lastUpdated",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                    }

                    FilledTonalButton(
                        onClick = viewModel::refreshBlocklist,
                        enabled = !isRefreshing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = Spacing.sm),
                                strokeWidth = Spacing.xs,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(strings.refreshing)
                        } else {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.padding(end = Spacing.xs),
                            )
                            Text(strings.refreshBlocklistButton)
                        }
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(top = Spacing.md)) }

            // ─── DNS ──────────────────────────────────────────────────────
            item { SectionHeader(strings.upstreamDnsSection) }

            item {
                AnimatedVisibility(visible = settings.blocklistSource.upstreamDns == null) {
                    Column(Modifier.padding(horizontal = Spacing.md)) {
                        var dnsInput by rememberSaveable { mutableStateOf(settings.dnsPrimary) }
                        val isValidDns = dnsInput.isEmpty() || IPV4_REGEX.matches(dnsInput)
                        OutlinedTextField(
                            value = dnsInput,
                            onValueChange = {
                                dnsInput = it
                                viewModel.setDns(it, settings.dnsSecondary)
                            },
                            label = { Text(strings.upstreamDnsLabel) },
                            placeholder = { Text(strings.upstreamDnsHint) },
                            isError = !isValidDns,
                            supportingText =
                                if (!isValidDns) {
                                    { Text(strings.errorInvalidDns, color = MaterialTheme.colorScheme.error) }
                                } else {
                                    null
                                },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(top = Spacing.md)) }

            // ─── Behaviour ────────────────────────────────────────────────
            item { SectionHeader(strings.behaviourSection) }

            item {
                SwitchRow(
                    title = strings.autoStartLabel,
                    subtitle = strings.autoStartSubtitle,
                    checked = settings.autoStartOnBoot,
                    onCheckedChange = viewModel::setAutoStart,
                )
            }

            item {
                SwitchRow(
                    title = strings.notificationStatsLabel,
                    subtitle = strings.notificationStatsSubtitle,
                    checked = settings.showNotificationStats,
                    onCheckedChange = viewModel::setNotificationStats,
                )
            }

            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }
}
