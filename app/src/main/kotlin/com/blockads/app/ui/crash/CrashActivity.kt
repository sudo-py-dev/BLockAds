package com.blockads.app.ui.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.blockads.app.MainActivity
import com.blockads.app.i18n.LocalStrings
import com.blockads.app.i18n.Strings
import com.blockads.app.ui.theme.BlockAdsTheme
import com.blockads.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.util.Locale

import android.view.WindowManager

class CrashActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CRASH_INFO = "crash_info"

        fun createIntent(
            context: Context,
            info: String,
        ): Intent =
            Intent(context, CrashActivity::class.java)
                .putExtra(EXTRA_CRASH_INFO, info)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        val crashInfo = intent.getStringExtra(EXTRA_CRASH_INFO) ?: "Unknown error"

        setContent {
            val strings =
                remember {
                    Strings.resolve(Locale.getDefault().language)
                }
            BlockAdsTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalStrings provides strings,
                ) {
                    CrashScreen(
                        crashInfo = crashInfo,
                        onCopy = { copyToClipboard(crashInfo) },
                        onRestart = { restartApp() },
                    )
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BlockAds crash log", text))
    }

    private fun restartApp() {
        val intent =
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashScreen(
    crashInfo: String,
    onCopy: () -> Unit,
    onRestart: () -> Unit,
) {
    val strings = LocalStrings.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val stackLines = remember(crashInfo) { crashInfo.lines() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.crashTitle) })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = Spacing.sm),
                )
                Text(
                    text = strings.crashSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = strings.crashDetails,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(Spacing.xs))

            ElevatedCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.padding(Spacing.sm),
                ) {
                    items(stackLines) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.md))

            Row(modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = {
                        onCopy()
                        scope.launch { snackbar.showSnackbar(strings.crashCopied) }
                    },
                    modifier = Modifier.weight(1f).padding(end = Spacing.sm),
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.padding(end = Spacing.xs))
                    Text(strings.crashCopy)
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.padding(end = Spacing.xs))
                    Text(strings.crashRestart)
                }
            }
        }
    }
}
