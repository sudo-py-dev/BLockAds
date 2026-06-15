package com.blockads.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SecurityUpdateWarning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    val isConnecting = vpnState == VpnState.CONNECTING
    val isActive = vpnState == VpnState.ACTIVE || vpnState == VpnState.PAUSED

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (isConnecting) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale",
        )
    } else {
        animateFloatAsState(targetValue = 1f, label = "staticScale")
    }

    val bgColor by animateColorAsState(
        targetValue =
            when (vpnState) {
                VpnState.ACTIVE -> MaterialTheme.colorScheme.primary
                VpnState.CONNECTING -> MaterialTheme.colorScheme.primaryContainer
                VpnState.ERROR -> MaterialTheme.colorScheme.errorContainer
                VpnState.STOPPED -> MaterialTheme.colorScheme.surfaceContainerHigh
                VpnState.PAUSED -> MaterialTheme.colorScheme.tertiary
            },
        animationSpec = tween(500),
        label = "bgColor",
    )

    val iconTint by animateColorAsState(
        targetValue =
            when (vpnState) {
                VpnState.ACTIVE -> MaterialTheme.colorScheme.onPrimary
                VpnState.CONNECTING -> MaterialTheme.colorScheme.onPrimaryContainer
                VpnState.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                VpnState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                VpnState.PAUSED -> MaterialTheme.colorScheme.onTertiary
            },
        animationSpec = tween(500),
        label = "iconTint",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.15f else 0f,
        animationSpec = tween(500),
        label = "glowAlpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(200.dp),
    ) {
        // Outer glow ring
        if (isActive || isConnecting) {
            Box(
                modifier =
                    Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(bgColor.copy(alpha = glowAlpha)),
            )
        }

        Surface(
            onClick = onToggle,
            enabled = vpnState != VpnState.CONNECTING,
            shape = CircleShape,
            color = bgColor,
            tonalElevation = 8.dp,
            shadowElevation = if (isActive) 12.dp else 4.dp,
            modifier =
                Modifier
                    .size(140.dp)
                    .scale(if (isConnecting) 1f else pulseScale)
                    .semantics {
                        contentDescription = contentDesc
                        role = Role.Button
                    },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector =
                        when (vpnState) {
                            VpnState.ERROR -> Icons.Rounded.SecurityUpdateWarning
                            VpnState.STOPPED -> Icons.Rounded.PowerSettingsNew
                            else -> Icons.Rounded.Security
                        },
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
    }
}
