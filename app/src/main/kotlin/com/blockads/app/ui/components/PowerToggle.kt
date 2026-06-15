package com.blockads.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SecurityUpdateWarning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blockads.app.domain.model.VpnState

@Composable
fun PowerToggle(
    vpnState: VpnState,
    onToggle: () -> Unit,
    contentDesc: String,
    modifier: Modifier = Modifier,
) {
    val isActive = vpnState == VpnState.ACTIVE
    val isConnecting = vpnState == VpnState.CONNECTING

    val pulseScale by if (isConnecting) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
            .let { state ->
                object : androidx.compose.runtime.State<Float> {
                    override val value: Float get() = state.floatValue
                }
            }
    }

    val bgColor: Color =
        when (vpnState) {
            VpnState.ACTIVE -> MaterialTheme.colorScheme.primary
            VpnState.CONNECTING -> MaterialTheme.colorScheme.primaryContainer
            VpnState.ERROR -> MaterialTheme.colorScheme.errorContainer
            VpnState.STOPPED -> MaterialTheme.colorScheme.surfaceVariant
            VpnState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
        }

    val iconTint: Color =
        when (vpnState) {
            VpnState.ACTIVE -> MaterialTheme.colorScheme.onPrimary
            VpnState.CONNECTING -> MaterialTheme.colorScheme.onPrimaryContainer
            VpnState.ERROR -> MaterialTheme.colorScheme.onErrorContainer
            VpnState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
            VpnState.PAUSED -> MaterialTheme.colorScheme.onTertiaryContainer
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .scale(pulseScale)
                .size(140.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(
                    enabled = vpnState != VpnState.CONNECTING,
                    onClick = onToggle,
                )
                .semantics {
                    contentDescription = contentDesc
                    role = Role.Button
                },
    ) {
        Icon(
            imageVector =
                if (vpnState == VpnState.ERROR) {
                    Icons.Rounded.SecurityUpdateWarning
                } else {
                    Icons.Rounded.Security
                },
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(72.dp),
        )
    }
}
