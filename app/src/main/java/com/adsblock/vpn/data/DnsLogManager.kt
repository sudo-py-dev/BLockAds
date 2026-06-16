package com.adsblock.vpn.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DnsLogEntry(
    val domain: String,
    val isBlocked: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = java.util.UUID.randomUUID().toString(),
)

object DnsLogManager {
    private const val MAX_LOGS = 200
    private val logQueue = java.util.concurrent.ConcurrentLinkedDeque<DnsLogEntry>()

    private val _logs = MutableStateFlow<List<DnsLogEntry>>(emptyList())
    val logs: StateFlow<List<DnsLogEntry>> = _logs.asStateFlow()

    fun addLog(
        domain: String,
        isBlocked: Boolean,
    ) {
        logQueue.addFirst(DnsLogEntry(domain, isBlocked))
        while (logQueue.size > MAX_LOGS) {
            logQueue.pollLast()
        }
        _logs.value = logQueue.toList()
    }

    fun clearLogs() {
        logQueue.clear()
        _logs.value = emptyList()
    }
}
