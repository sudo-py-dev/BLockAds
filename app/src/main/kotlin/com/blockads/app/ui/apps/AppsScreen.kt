package com.blockads.app.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.blockads.app.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val appsUiState by viewModel.appsUiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.bypassTitle, fontWeight = FontWeight.Bold) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item {
                    Text(
                        text = strings.bypassSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                items(appsUiState, key = { it.first.packageName }) { (app, isBypassed) ->
                    AppListItem(
                        app = app,
                        isBypassed = isBypassed,
                        onToggle = { bypass ->
                            viewModel.toggleAppBypass(app.packageName, bypass)
                        },
                        strings = strings,
                    )
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppItem,
    isBypassed: Boolean,
    onToggle: (Boolean) -> Unit,
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
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.name,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(modifier = Modifier.size(48.dp)) // Placeholder
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (app.isSystemApp) {
                    Text(
                        text = strings.systemApp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isBypassed,
                onCheckedChange = onToggle,
            )
        }
    }
}
