package com.blockads.app.domain.model

data class DnsStats(
    val blockedCount: Long = 0L,
    val forwardedCount: Long = 0L,
    val sessionStartMs: Long = 0L,
) {
    val uptimeMs: Long
        get() = if (sessionStartMs > 0L) System.currentTimeMillis() - sessionStartMs else 0L
}
