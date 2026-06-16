package com.adsblock.vpn.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DnsStatsManager {
    private val _totalQueries = MutableStateFlow(0)
    val totalQueries: StateFlow<Int> = _totalQueries.asStateFlow()

    private val _blockedQueries = MutableStateFlow(0)
    val blockedQueries: StateFlow<Int> = _blockedQueries.asStateFlow()

    fun incrementTotal() {
        _totalQueries.value++
    }

    fun incrementBlocked() {
        _blockedQueries.value++
    }

    fun reset() {
        _totalQueries.value = 0
        _blockedQueries.value = 0
    }
}
