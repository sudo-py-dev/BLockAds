package com.blockads.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.blockads.app.domain.model.DnsStats
import com.blockads.app.i18n.LocalizedStrings
import com.blockads.app.ui.theme.Spacing
import java.util.concurrent.TimeUnit

@Composable
fun StatsCard(
    stats: DnsStats,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Spacing.xs),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatColumn(
                label = strings.blockedCount,
                value = stats.blockedCount.formatCompact(),
            )
            StatColumn(
                label = strings.forwardedCount,
                value = stats.forwardedCount.formatCompact(),
            )
            StatColumn(
                label = strings.sessionUptime,
                value = stats.uptimeMs.formatUptime(),
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Long.formatCompact(): String =
    when {
        this >= 1_000_000 -> "${this / 1_000_000}M"
        this >= 1_000 -> "${this / 1_000}K"
        else -> "$this"
    }

private fun Long.formatUptime(): String {
    val h = TimeUnit.MILLISECONDS.toHours(this)
    val m = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return if (h > 0) "%dh %02dm".format(h, m) else "%dm %02ds".format(m, s)
}
