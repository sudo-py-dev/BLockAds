package com.blockads.app.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockads.app.BuildConfig
import com.blockads.app.i18n.LocalStrings
import com.blockads.app.ui.theme.Spacing

private data class OpenSourceLib(
    val name: String,
    val license: String,
    val url: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current

    val libs =
        listOf(
            OpenSourceLib("Jetpack Compose", strings.aboutLicenseApache, "https://developer.android.com/jetpack/compose"),
            OpenSourceLib("Hilt (Dagger)", strings.aboutLicenseApache, "https://dagger.dev/hilt/"),
            OpenSourceLib("OkHttp", strings.aboutLicenseApache, "https://square.github.io/okhttp/"),
            OpenSourceLib("Kotlinx Coroutines", strings.aboutLicenseApache, "https://github.com/Kotlin/kotlinx.coroutines"),
            OpenSourceLib("Kotlinx Serialization", strings.aboutLicenseApache, "https://github.com/Kotlin/kotlinx.serialization"),
            OpenSourceLib(
                "Jetpack DataStore",
                strings.aboutLicenseApache,
                "https://developer.android.com/topic/libraries/architecture/datastore",
            ),
            OpenSourceLib("Steven Black hosts", strings.aboutLicenseMit, "https://github.com/StevenBlack/hosts"),
            OpenSourceLib("AdGuard DNS Filter", strings.aboutLicenseGpl, "https://github.com/AdguardTeam/AdGuardSDNSFilter"),
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutTitle) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.md),
        ) {
            // App identity card
            item {
                Spacer(Modifier.height(Spacing.md))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(Spacing.xs),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${strings.aboutVersion} ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = strings.aboutDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Developer
            item {
                Spacer(Modifier.height(Spacing.md))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(Spacing.xs),
                ) {
                    ListItem(
                        headlineContent = { Text(strings.aboutDeveloper) },
                        supportingContent = { Text(strings.aboutDeveloperName) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(strings.aboutViewOnGithub) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier =
                            Modifier.clickable {
                                uriHandler.openUri("https://github.com/blockads-app/blockads")
                            },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(strings.aboutReportIssue) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier =
                            Modifier.clickable {
                                uriHandler.openUri("https://github.com/blockads-app/blockads/issues")
                            },
                    )
                }
            }

            // Open-source credits
            item {
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = strings.aboutOpenSource,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(Spacing.xs),
                ) {
                    libs.forEachIndexed { idx, lib ->
                        ListItem(
                            headlineContent = { Text(lib.name) },
                            supportingContent = { Text(lib.license) },
                            trailingContent = {
                                Icon(
                                    Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.clickable { uriHandler.openUri(lib.url) },
                        )
                        if (idx < libs.lastIndex) HorizontalDivider()
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}
